package br.iamonitor.desktop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

final class CodexRunner implements AutoCloseable {
  private static final ObjectMapper JSON = new ObjectMapper();
  private final Store store;
  private final Runnable onChange;
  private final List<String> injectedCommand;
  private final CodexQuotaReader quotaReader;
  private final ExecutorService workers = Executors.newCachedThreadPool(r -> {
    Thread thread = new Thread(r, "iamonitor-codex");
    thread.setDaemon(true);
    return thread;
  });
  private final ScheduledExecutorService sampler = Executors.newSingleThreadScheduledExecutor(r -> {
    Thread thread = new Thread(r, "iamonitor-sampler");
    thread.setDaemon(true);
    return thread;
  });
  private final Map<String, Active> active = new ConcurrentHashMap<>();

  private static final class Active {
    final Process process;
    final Path project;
    final StringBuilder stderr = new StringBuilder();
    volatile boolean interrupted;
    volatile String captureError;
    volatile double previousCpu;
    volatile long previousAt;
    ScheduledFuture<?> sampling;
    Active(Process process, Path project) { this.process = process; this.project = project; }
  }

  CodexRunner(Store store, Runnable onChange) {
    this(store, onChange, null);
  }

  CodexRunner(Store store, Runnable onChange, List<String> injectedCommand) {
    this(store, onChange, injectedCommand, injectedCommand == null ? new CodexQuotaReader() : null);
  }

  CodexRunner(Store store, Runnable onChange, List<String> injectedCommand, CodexQuotaReader quotaReader) {
    this.store = store;
    this.onChange = onChange;
    this.injectedCommand = injectedCommand == null ? null : List.copyOf(injectedCommand);
    this.quotaReader = quotaReader;
  }

  int activeCount() { return active.size(); }
  boolean isActive(String id) { return active.containsKey(id); }

  synchronized Store.Run start(Path project, String prompt, String model, String sandbox) throws IOException, SQLException {
    return start(project, prompt, model, sandbox, "codex");
  }

  synchronized Store.Run start(Path project, String prompt, String model, String sandbox, String provider) throws IOException, SQLException {
    Path root = project.toAbsolutePath().normalize();
    if (!Files.isDirectory(root)) throw new IllegalArgumentException("Escolha uma pasta de projeto existente.");
    if (prompt.isBlank() || prompt.length() > 50000) throw new IllegalArgumentException("Escreva um pedido de até 50 mil caracteres.");
    if (!Set.of("read-only", "workspace-write").contains(sandbox)) throw new IllegalArgumentException("Permissão inválida.");
    if (active.size() >= 3) throw new IllegalStateException("Limite de três sessões simultâneas.");
    if (!Provider.IDS.contains(provider)) throw new IllegalArgumentException("Ferramenta desconhecida.");
    String request = prompt.strip();
    Store.Run run = store.begin(root, request, model.strip(), sandbox, provider);
    launch(run);
    return run;
  }

  synchronized Store.Run restart(String id) throws IOException, SQLException {
    if (active.containsKey(id)) throw new IllegalStateException("Interrompa a sessão antes de reiniciar.");
    if (active.size() >= 3) throw new IllegalStateException("Limite de três sessões simultâneas.");
    Store.Run previous = store.run(id);
    if (previous == null) throw new IllegalArgumentException("Sessão não encontrada.");
    if (!Files.isDirectory(Path.of(previous.project()))) throw new IllegalArgumentException("A pasta do projeto não existe mais.");
    store.restart(id);
    Store.Run run = store.run(id);
    launch(run);
    return run;
  }

  private void launch(Store.Run run) throws IOException, SQLException {
    Path root = Path.of(run.project());
    String request = run.prompt();
    String model = run.model() == null ? "" : run.model();
    String sandbox = run.sandbox();
    readQuota(run.id(), run.provider(), "before");
    try {
      List<String> command = injectedCommand == null ? Provider.command(run.provider(), root, model, sandbox) : new ArrayList<>(injectedCommand);
      if (injectedCommand != null) command.addAll(Provider.commandArguments(run.provider(), root, model, sandbox));
      for (int i = 0; i < command.size(); i++) if (command.get(i).isEmpty()) command.set(i, request);
      if ("claude".equals(run.provider()) || "cursor".equals(run.provider()) && !Provider.shellWrapper(run.provider())) command.add(request);
      ProcessBuilder builder = new ProcessBuilder(command).directory(root.toFile());
      if (Provider.shellWrapper(run.provider()) && injectedCommand == null) {
        builder.environment().put("IAMONITOR_EXEC", Provider.executable(run.provider()));
        builder.environment().put("IAMONITOR_MODEL", model);
        builder.environment().put("IAMONITOR_SANDBOX", sandbox);
      }
      Process process = builder.start();
      Active state = new Active(process, root);
      active.put(run.id(), state);
      store.observeArtifacts(run.id(), request, root);
      store.observeArtifacts(run.id(), "\"" + root.resolve("AGENTS.md") + "\"", root);
      workers.submit(() -> {
        try (var writer = process.outputWriter(StandardCharsets.UTF_8)) { if ("codex".equals(run.provider()) || Provider.shellWrapper(run.provider()) && injectedCommand == null) writer.write(request); }
        catch (IOException ignored) { /* The subprocess may have exited. */ }
      });
      workers.submit(() -> readStderr(state));
      Future<?> eventReader = workers.submit(() -> readEvents(run.id(), run.provider(), state));
      state.sampling = sampler.scheduleAtFixedRate(() -> sample(run.id(), state), 0, 3, TimeUnit.SECONDS);
      workers.submit(() -> awaitFinish(run.id(), run.provider(), state, eventReader));
      onChange.run();
    } catch (IOException | RuntimeException error) {
      store.finish(run.id(), "failed", null, error.getMessage());
      onChange.run();
      throw error;
    }
  }

  private void readQuota(String id, String provider, String phase) {
    if (!"codex".equals(provider) || quotaReader == null) return;
    try { store.saveQuotaSnapshots(id, quotaReader.read(phase)); }
    catch (IOException | SQLException ignored) { /* Cota indisponível não impede a execução. */ }
    onChange.run();
  }

  static List<String> codexCommand() {
    String configured = System.getenv("IAMONITOR_CODEX_BIN");
    if (configured != null && !configured.isBlank() && Files.isRegularFile(Path.of(configured))) {
      if (configured.matches("(?i).*\\.[cm]?js")) return new ArrayList<>(List.of("node", configured));
      if (configured.toLowerCase().endsWith(".cmd") || configured.toLowerCase().endsWith(".bat")) return new ArrayList<>(List.of("cmd.exe", "/c", configured));
      return new ArrayList<>(List.of(configured));
    }
    if (System.getProperty("os.name").toLowerCase().contains("win")) {
      String appData = System.getenv("APPDATA");
      if (appData != null) {
        Path script = Path.of(appData, "npm", "node_modules", "@openai", "codex", "bin", "codex.js");
        if (Files.isRegularFile(script)) return new ArrayList<>(List.of("node", script.toString()));
      }
      return new ArrayList<>(List.of("cmd.exe", "/c", "codex"));
    }
    return new ArrayList<>(List.of("codex"));
  }

  private void readEvents(String id, String provider, Active state) {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(state.process.getInputStream(), StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.isBlank()) continue;
        JsonNode event;
        try { event = JSON.readTree(line); }
        catch (Exception error) {
          ObjectNode warning = JSON.createObjectNode();
          warning.put("type", "parse.warning");
          warning.put("message", line.substring(0, Math.min(1000, line.length())));
          event = warning;
        }
        event = Provider.normalize(provider, event);
        store.recordEvent(id, event, JSON.writeValueAsString(event));
        if (!"codex".equals(provider)) store.observeArtifacts(id, JSON.writeValueAsString(event), state.project);
        JsonNode item = event.path("item");
        if ("command_execution".equals(item.path("type").asText())) {
          store.observeArtifacts(id, item.path("command").asText(), state.project);
        } else if ("file_change".equals(item.path("type").asText())) {
          for (JsonNode change : item.path("changes")) {
            store.observeArtifacts(id, "\"" + change.path("path").asText() + "\"", state.project);
          }
        }
        onChange.run();
      }
    } catch (Exception error) {
      state.captureError = "Leitura do fluxo: " + error.getMessage();
      synchronized (state.stderr) { state.stderr.append('\n').append(state.captureError); }
    }
  }

  private static void readStderr(Active state) {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(state.process.getErrorStream(), StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        synchronized (state.stderr) {
          state.stderr.append(line).append('\n');
          if (state.stderr.length() > 8000) state.stderr.delete(0, state.stderr.length() - 8000);
        }
      }
    } catch (IOException ignored) { /* The process may have exited. */ }
  }

  private void awaitFinish(String id, String provider, Active state, Future<?> eventReader) {
    try {
      int code = state.process.waitFor();
      eventReader.get();
      if (state.sampling != null) state.sampling.cancel(false);
      String error;
      synchronized (state.stderr) { error = state.stderr.toString().strip(); }
      store.finish(id, state.interrupted ? "interrupted" : code == 0 && state.captureError == null ? "completed" : "failed",
          code, state.captureError != null ? state.captureError : code == 0 ? null : error);
    } catch (InterruptedException error) {
      Thread.currentThread().interrupt();
      try { store.finish(id, "interrupted", null, "Processo interrompido."); } catch (SQLException ignored) {}
    } catch (Exception error) {
      try { store.finish(id, state.interrupted ? "interrupted" : "failed", null, error.getMessage()); }
      catch (SQLException ignored) { /* The UI will report a database error on refresh. */ }
    } finally {
      readQuota(id, provider, "after");
      active.remove(id);
      onChange.run();
    }
  }

  private void sample(String id, Active state) {
    if (!System.getProperty("os.name").toLowerCase().contains("win") || !state.process.isAlive()) return;
    try {
      List<Long> ids = new ArrayList<>();
      ids.add(state.process.pid());
      state.process.descendants().forEach(child -> ids.add(child.pid()));
      String script = "Get-Process -Id " + String.join(",", ids.stream().map(String::valueOf).toList())
          + " -ErrorAction SilentlyContinue | Select-Object CPU,WorkingSet64 | ConvertTo-Json -Compress";
      Process query = new ProcessBuilder("powershell.exe", "-NoProfile", "-Command", script).start();
      if (!query.waitFor(2500, TimeUnit.MILLISECONDS)) { query.destroyForcibly(); return; }
      String json = new String(query.getInputStream().readAllBytes(), StandardCharsets.UTF_8).strip();
      if (json.isEmpty()) return;
      JsonNode values = JSON.readTree(json);
      double cpuSeconds = 0;
      long memory = 0;
      for (JsonNode value : values.isArray() ? values : List.of(values)) {
        cpuSeconds += value.path("CPU").asDouble(0);
        memory += value.path("WorkingSet64").asLong(0);
      }
      long now = System.currentTimeMillis();
      double cpu = state.previousAt == 0 ? 0 : Math.max(0,
          (cpuSeconds - state.previousCpu) * 100000.0 / (now - state.previousAt) / Runtime.getRuntime().availableProcessors());
      state.previousCpu = cpuSeconds;
      state.previousAt = now;
      store.sample(id, cpu, memory);
      onChange.run();
    } catch (Exception ignored) { /* One missed sample does not fail the session. */ }
  }

  void stop(String id) {
    Active state = active.get(id);
    if (state == null) return;
    state.interrupted = true;
    try { store.finish(id, "interrupted", null, null); } catch (SQLException ignored) {}
    state.process.descendants().forEach(child -> child.destroyForcibly());
    state.process.destroyForcibly();
  }

  @Override public void close() {
    for (String id : List.copyOf(active.keySet())) stop(id);
    sampler.shutdownNow();
    workers.shutdown();
    try {
      if (!workers.awaitTermination(5, TimeUnit.SECONDS)) workers.shutdownNow();
    } catch (InterruptedException error) {
      workers.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }
}

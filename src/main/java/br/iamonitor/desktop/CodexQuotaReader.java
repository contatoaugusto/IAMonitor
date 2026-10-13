package br.iamonitor.desktop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Reads official ChatGPT quota windows without starting an inference turn. */
final class CodexQuotaReader {
  private static final ObjectMapper JSON = new ObjectMapper();
  private final List<String> command;

  CodexQuotaReader() {
    List<String> configured = CodexRunner.codexCommand();
    command = new ArrayList<>(configured);
    command.add("app-server");
  }

  CodexQuotaReader(List<String> command) { this.command = List.copyOf(command); }

  List<Store.QuotaSnapshot> read(String phase) throws IOException {
    if (!List.of("before", "after").contains(phase)) throw new IllegalArgumentException("Fase inválida");
    Process process = new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.DISCARD).start();
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var future = executor.submit(() -> exchange(process, phase));
      try {
        return future.get(8, TimeUnit.SECONDS);
      } catch (TimeoutException error) {
        stop(process);
        throw new IOException("Tempo esgotado na consulta de cota", error);
      } catch (InterruptedException error) {
        stop(process);
        Thread.currentThread().interrupt();
        throw new IOException("Consulta de cota interrompida", error);
      } catch (ExecutionException error) {
        if (error.getCause() instanceof IOException io) throw io;
        throw new IOException("Falha na consulta de cota", error.getCause());
      }
    } finally {
      stop(process);
    }
  }

  private static void stop(Process process) {
    process.descendants().forEach(ProcessHandle::destroyForcibly);
    process.destroyForcibly();
  }

  private static List<Store.QuotaSnapshot> exchange(Process process, String phase) throws IOException {
    try (var writer = process.outputWriter(StandardCharsets.UTF_8);
         var reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
      writer.write("{\"method\":\"initialize\",\"id\":1,\"params\":{\"clientInfo\":{\"name\":\"iamonitor\",\"title\":\"IAMonitor\",\"version\":\"1.0.2\"}}}\n");
      writer.flush();
      response(reader, 1);
      writer.write("{\"method\":\"initialized\",\"params\":{}}\n");
      writer.write("{\"method\":\"account/rateLimits/read\",\"id\":2}\n");
      writer.flush();
      JsonNode result = response(reader, 2);
      return parse(result, phase, Instant.now().toString());
    }
  }

  private static JsonNode response(BufferedReader reader, int id) throws IOException {
    String line;
    while ((line = reader.readLine()) != null) {
      JsonNode message;
      try { message = JSON.readTree(line); }
      catch (Exception ignored) { continue; }
      if (message.path("id").asInt(-1) != id) continue;
      if (message.has("error")) throw new IOException("Codex não disponibilizou a cota");
      JsonNode result = message.path("result");
      if (!result.isObject()) throw new IOException("Resposta de cota inválida");
      return result;
    }
    throw new IOException("Codex encerrou a consulta de cota");
  }

  static List<Store.QuotaSnapshot> parse(JsonNode result, String phase, String capturedAt) {
    List<Store.QuotaSnapshot> snapshots = new ArrayList<>();
    JsonNode buckets = result.path("rateLimitsByLimitId");
    if (buckets.isObject() && !buckets.isEmpty()) {
      var fields = buckets.fields();
      while (fields.hasNext()) {
        Map.Entry<String, JsonNode> entry = fields.next();
        addWindows(snapshots, entry.getKey(), entry.getValue(), phase, capturedAt);
      }
    } else {
      JsonNode bucket = result.path("rateLimits");
      if (bucket.isObject()) addWindows(snapshots, bucket.path("limitId").asText("codex"), bucket, phase, capturedAt);
    }
    return List.copyOf(snapshots);
  }

  private static void addWindows(List<Store.QuotaSnapshot> snapshots, String limitId, JsonNode bucket,
                                 String phase, String capturedAt) {
    for (String window : List.of("primary", "secondary")) {
      JsonNode value = bucket.path(window);
      if (!value.isObject()) continue;
      double used = value.path("usedPercent").asDouble(Double.NaN);
      int minutes = value.path("windowDurationMins").asInt(0);
      long resetsAt = value.path("resetsAt").asLong(0);
      if (limitId.isBlank() || !Double.isFinite(used) || used < 0 || used > 100
          || minutes <= 0 || resetsAt <= 0) continue;
      snapshots.add(new Store.QuotaSnapshot(phase, limitId, window, minutes,
          resetsAt, used, capturedAt, bucket.path("planType").asText("")));
    }
  }
}

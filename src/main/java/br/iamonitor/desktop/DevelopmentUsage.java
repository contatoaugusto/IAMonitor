package br.iamonitor.desktop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

final class DevelopmentUsage {
  record Result(int sessions, long input, long cached, long output, long total) {}
  record Sample(int line, long input, long cached, long output, long total) {}
  record Entry(Path file, List<Sample> samples) {
    Sample last() { return samples.getLast(); }
  }
  record Breakdown(Result summary, List<Entry> entries) {}
  private static final ObjectMapper JSON = new ObjectMapper();

  private DevelopmentUsage() {}

  static Result scan(Path sessionsRoot, Path projectRoot) throws IOException {
    return scanDetails(sessionsRoot, projectRoot).summary();
  }

  static Breakdown scanDetails(Path sessionsRoot, Path projectRoot) throws IOException {
    if (!Files.isDirectory(sessionsRoot))
      return new Breakdown(new Result(0, 0, 0, 0, 0), List.of());
    Result sum = new Result(0, 0, 0, 0, 0);
    List<Entry> entries = new ArrayList<>();
    try (Stream<Path> paths = Files.walk(sessionsRoot)) {
      for (Path file : paths.filter(path -> Files.isRegularFile(path)
          && path.getFileName().toString().endsWith(".jsonl")).toList()) {
        Entry entry = read(file, projectRoot);
        if (entry == null) continue;
        entries.add(entry);
        Sample usage = entry.last();
        sum = new Result(sum.sessions() + 1, sum.input() + usage.input(),
            sum.cached() + usage.cached(), sum.output() + usage.output(), sum.total() + usage.total());
      }
    }
    entries.sort(java.util.Comparator.comparing(entry -> entry.file().toString()));
    return new Breakdown(sum, List.copyOf(entries));
  }

  private static Entry read(Path file, Path projectRoot) {
    try (BufferedReader lines = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
      String first = lines.readLine();
      if (first == null) return null;
      JsonNode firstEvent = JSON.readTree(first);
      if (!"session_meta".equals(firstEvent.path("type").asText())) return null;
      JsonNode meta = firstEvent.path("payload");
      String cwd = meta.path("cwd").asText();
      if (cwd.isBlank() || !Path.of(cwd).toAbsolutePath().normalize().toString()
          .equalsIgnoreCase(projectRoot.toAbsolutePath().normalize().toString())) return null;
      long input = 0, cached = 0, output = 0, total = 0;
      List<Sample> samples = new ArrayList<>();
      String line;
      int lineNumber = 1;
      while ((line = lines.readLine()) != null) {
        lineNumber++;
        if (!line.contains("token_count")) continue;
        JsonNode payload;
        try { payload = JSON.readTree(line).path("payload"); }
        catch (IOException ignored) { continue; }
        if (!"token_count".equals(payload.path("type").asText())) continue;
        JsonNode usage = payload.path("info").path("total_token_usage");
        if (!usage.isObject()) continue;
        long reported = usage.path("total_tokens").asLong(-1);
        if (reported < total || reported < 0) continue;
        long nextInput = usage.path("input_tokens").asLong(-1);
        long nextCached = usage.path("cached_input_tokens").asLong(0);
        long nextOutput = usage.path("output_tokens").asLong(-1);
        if (nextInput < 0 || nextCached < 0 || nextOutput < 0
            || nextCached > nextInput || reported < nextInput + nextOutput) continue;
        total = reported;
        input = nextInput;
        cached = nextCached;
        output = nextOutput;
        samples.add(new Sample(lineNumber, input, cached, output, total));
      }
      return total > 0 ? new Entry(file, List.copyOf(samples)) : null;
    } catch (IOException | RuntimeException ignored) {
      return null;
    }
  }
}

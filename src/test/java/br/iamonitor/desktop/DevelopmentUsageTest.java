package br.iamonitor.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DevelopmentUsageTest {
  private static final ObjectMapper JSON = new ObjectMapper();

  @Test void somaUltimoAcumuladoPorSessaoSemDuplicarCache(@TempDir Path temp) throws Exception {
    Path project = Files.createDirectory(temp.resolve("project"));
    Path other = Files.createDirectory(temp.resolve("other"));
    Path logs = Files.createDirectory(temp.resolve("logs"));
    write(logs.resolve("one.jsonl"), project, new long[][]{{80, 30, 20, 100}, {140, 70, 40, 180}});
    write(logs.resolve("two.jsonl"), project, new long[][]{{20, 5, 5, 25}});
    write(logs.resolve("unrelated.jsonl"), other, new long[][]{{1000, 500, 200, 1200}});
    DevelopmentUsage.Result result = DevelopmentUsage.scan(logs, project);
    assertEquals(2, result.sessions());
    assertEquals(160, result.input());
    assertEquals(75, result.cached());
    assertEquals(45, result.output());
    assertEquals(205, result.total());
    DevelopmentUsage.Breakdown detail = DevelopmentUsage.scanDetails(logs, project);
    assertEquals(result, detail.summary());
    assertEquals(2, detail.entries().size());
    assertEquals("one.jsonl", detail.entries().get(0).file().getFileName().toString());
    assertEquals(2, detail.entries().get(0).samples().size());
    assertEquals(3, detail.entries().get(0).samples().get(1).line());
    assertEquals(70, detail.entries().get(0).last().cached());
    assertEquals(5, detail.entries().get(1).last().cached());
  }

  private static void write(Path file, Path cwd, long[][] counts) throws Exception {
    StringBuilder lines = new StringBuilder();
    lines.append(JSON.writeValueAsString(Map.of("type", "session_meta", "payload", Map.of("cwd", cwd.toString())))).append('\n');
    for (long[] values : counts) {
      var usage = Map.of("input_tokens", values[0], "cached_input_tokens", values[1],
          "output_tokens", values[2], "total_tokens", values[3]);
      lines.append(JSON.writeValueAsString(Map.of("type", "event_msg", "payload", Map.of("type", "token_count",
          "info", Map.of("total_token_usage", usage))))).append('\n');
    }
    Files.writeString(file, lines);
  }
}

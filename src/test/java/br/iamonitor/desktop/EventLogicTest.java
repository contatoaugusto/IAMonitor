package br.iamonitor.desktop;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EventLogicTest {
  private final ObjectMapper json = new ObjectMapper();

  @Test void somaUsoSemDuplicarCacheERaciocinio() throws Exception {
    var event = json.readTree("""
        {"type":"turn.completed","usage":{"input_tokens":120,"cached_input_tokens":20,
        "output_tokens":30,"reasoning_tokens":5,"total_tokens":150}}""");
    assertEquals(new EventLogic.Usage(120, 20, 30, 5, 150), EventLogic.usage(event));
    assertNull(EventLogic.usage(json.readTree("{\"type\":\"turn.completed\"}")));
    assertEquals(150, EventLogic.usage(json.readTree(
        "{\"type\":\"turn.completed\",\"usage\":{\"input_tokens\":120,\"output_tokens\":30}}")).total());
  }

  @Test void reconheceRaciocinioDoCodexERejeitaUsoIncompleto() throws Exception {
    var codex = json.readTree("""
        {"type":"turn.completed","usage":{"input_tokens":100,"cached_input_tokens":40,
        "output_tokens":25,"reasoning_output_tokens":7}}""");
    assertEquals(new EventLogic.Usage(100, 40, 25, 7, 125), EventLogic.usage(codex));
    assertNull(EventLogic.usage(json.readTree("""
        {"type":"turn.completed","usage":{"input_tokens":100}}
        """)));
    assertNull(EventLogic.usage(json.readTree("""
        {"type":"turn.completed","usage":{"input_tokens":-1,"output_tokens":10}}
        """)));
  }
  @Test void observaSomenteArquivosDoProjeto(@TempDir Path project) throws Exception {
    Path file = Files.writeString(project.resolve("spec.md"), "conteúdo");
    Path outside = Files.writeString(project.getParent().resolve("outside-test.md"), "fora");
    try {
      assertEquals(List.of(file), EventLogic.pathsFromText(
          "Get-Content \"spec.md\"; Get-Content \"" + outside + "\"", project));
      assertEquals("Spec", EventLogic.category(file));
      assertTrue(new EventLogic().tokens("conteúdo") > 0);
    } finally {
      Files.deleteIfExists(outside);
    }
  }

  @Test void resumeComandoLocal() throws Exception {
    var event = json.readTree("{\"type\":\"item.completed\",\"item\":{\"type\":\"command_execution\",\"command\":\"Get-Content README.md\"}}");
    assertEquals("local", EventLogic.summarize(event).kind());
    assertEquals("Comando local concluído", EventLogic.summarize(event).title());
  }

  @Test void classificaArquivoDeComandoSemConfundirEventoLocal() {
    assertEquals("Command", EventLogic.category(Path.of(".claude/commands/revisar.md")));
    assertEquals("Command", EventLogic.category(Path.of("tarefas/revisar.command.md")));
    assertEquals("command", EventLogic.sourceKey("Command"));
    assertEquals("Arquivo", EventLogic.category(Path.of("src/main.java")));
  }
}

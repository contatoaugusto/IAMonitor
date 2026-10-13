package br.iamonitor.desktop;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProviderTest {
  private final ObjectMapper json = new ObjectMapper();

  @Test void normalizaUsoFinalClaudeSemSomarCacheDuasVezes() throws Exception {
    var source = json.readTree("""
      {"type":"result","usage":{"input_tokens":9,"cache_read_input_tokens":40,"cache_creation_input_tokens":11,"output_tokens":20},
       "modelUsage":{"sonnet":{"inputTokens":9,"cacheReadInputTokens":40,"cacheCreationInputTokens":11,"outputTokens":20}}}
      """);
    var usage = EventLogic.usage(Provider.normalize("claude", source));
    assertNotNull(usage);
    assertEquals(60, usage.input());
    assertEquals(40, usage.cached());
    assertEquals(20, usage.output());
    assertEquals(80, usage.total());
  }

  @Test void normalizaResultadoGeminiPreservandoTotalInformado() throws Exception {
    var source = json.readTree("""
      {"type":"result","stats":{"input_tokens":100,"output_tokens":20,"cached":30,"total_tokens":124}}
      """);
    var usage = EventLogic.usage(Provider.normalize("gemini", source));
    assertNotNull(usage);
    assertEquals(100, usage.input());
    assertEquals(30, usage.cached());
    assertEquals(124, usage.total());
  }

  @Test void cursorECopilotNaoInventamTokens() throws Exception {
    for (String provider : new String[]{"cursor", "copilot"}) {
      var source = json.readTree("{\"type\":\"result\",\"result\":\"ok\"}");
      assertNull(EventLogic.usage(Provider.normalize(provider, source)));
    }
  }

  @Test void comandosUsamFormatosEstruturados(@org.junit.jupiter.api.io.TempDir Path temp) {
    assertTrue(Provider.command("codex", temp, "", "read-only").contains("--json"));
    assertTrue(Provider.command("claude", temp, "", "read-only").contains("stream-json"));
    assertTrue(String.join(" ", Provider.command("gemini", temp, "", "read-only")).contains("stream-json"));
    assertTrue(String.join(" ", Provider.command("cursor", temp, "", "read-only")).contains("stream-json"));
    assertTrue(String.join(" ", Provider.command("copilot", temp, "", "read-only")).contains("json"));
  }
  @Test void explicaLoginExecucaoEAlcanceDaColetaParaCadaIA() {
    for (String id : Provider.IDS) {
      String explanation = Provider.explanation(id);
      assertTrue(explanation.contains(Provider.name(id)));
      assertTrue(explanation.contains("PowerShell"));
      assertTrue(explanation.contains("Não precisa entrar na pasta"));
      assertTrue(explanation.contains("inicia uma nova execução da CLI"));
      assertTrue(explanation.contains("não lê nem copia credenciais"));
      assertTrue(explanation.contains("Não se conecta a sessões já abertas"));
      assertTrue(explanation.contains("não equivale necessariamente"));
    }
    assertTrue(Provider.explanation("cursor").contains("agent login"));
    assertTrue(Provider.explanation("copilot").contains("copilot login"));
    assertTrue(Provider.explanation("gemini").contains("Sign in with Google"));
  }
}

package br.iamonitor.desktop;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CacheEvidenceTest {
  @Test void separaResultadosEEventosSemAtribuirConteudoAoCache(@TempDir Path temp) throws Exception {
    Path project = Files.createDirectory(temp.resolve("project"));
    try (Store store = new Store(temp.resolve("data"), new EventLogic())) {
      Store.Run run = store.begin(project, "Pedido de teste", "", "read-only");
      ObjectMapper json = new ObjectMapper();
      record(store, run.id(), json, "{\"type\":\"turn.started\"}");
      record(store, run.id(), json, "{\"type\":\"item.completed\",\"item\":{\"type\":\"agent_message\",\"text\":\"Mensagem observada\"}}");
      record(store, run.id(), json, "{\"type\":\"turn.completed\",\"usage\":{\"input_tokens\":120,\"cached_input_tokens\":30,\"output_tokens\":20,\"total_tokens\":140}}");
      record(store, run.id(), json, "{\"type\":\"turn.started\"}");
      record(store, run.id(), json, "{\"type\":\"turn.completed\",\"usage\":{\"input_tokens\":200,\"cached_input_tokens\":80,\"output_tokens\":40,\"total_tokens\":240}}");
      Store.Detail detail = store.reportDetail(run.id());
      Map<Integer, String> payloads = new HashMap<>();
      for (Store.Event event : detail.events()) {
        String payload = store.eventPayload(run.id(), event.seq());
        assertNotNull(payload);
        payloads.put(event.seq(), payload);
      }
      assertNull(store.eventPayload(run.id(), 999));
      CacheEvidence.Snapshot evidence = CacheEvidence.from(detail, payloads, true);
      assertEquals(2, evidence.units().size());
      assertEquals(110, evidence.measuredCache());
      assertEquals(1, evidence.units().get(0).startSequence());
      assertEquals(30, evidence.units().get(0).parts().cache());
      assertEquals(90, evidence.units().get(0).parts().inputWithoutCache());
      assertEquals(2, evidence.units().get(0).interval().size());
      assertEquals(4, evidence.units().get(1).startSequence());
      assertEquals(80, evidence.units().get(1).providerFields().get(0).tokens());
    }
  }

  @Test void distingueLeituraECriacaoDeCacheNoClaude() {
    String payload = """
        {"provider_event":{"modelUsage":{"sonnet":{"cacheReadInputTokens":40,
        "cacheCreationInputTokens":11},"haiku":{"cacheReadInputTokens":5,
        "cacheCreationInputTokens":2}}}}""";
    var fields = CacheEvidence.providerFields("claude", payload);
    assertEquals(4, fields.size());
    assertEquals(40, fields.get(0).tokens());
    assertTrue(fields.get(1).label().contains("criado"));
    assertEquals(11, fields.get(1).tokens());
    assertEquals(5, fields.get(2).tokens());
    assertEquals(2, fields.get(3).tokens());
    assertEquals(30, CacheEvidence.providerFields("gemini",
        "{\"provider_event\":{\"stats\":{\"cached\":30}}}").get(0).tokens());
    assertTrue(CacheEvidence.providerFields("codex", "inválido").isEmpty());
  }

  @Test void distingueZeroInformadoDeContagemAusente(@TempDir Path temp) throws Exception {
    Path project = Files.createDirectory(temp.resolve("project"));
    try (Store store = new Store(temp.resolve("data"), new EventLogic())) {
      Store.Run run = store.begin(project, "Pedido fictício", "", "read-only");
      ObjectMapper json = new ObjectMapper();
      record(store, run.id(), json,
          "{\"type\":\"turn.completed\",\"usage\":{\"input_tokens\":20,\"cached_input_tokens\":0,\"output_tokens\":5,\"total_tokens\":25}}");
      record(store, run.id(), json, "{\"type\":\"turn.completed\"}");
      CacheEvidence.Snapshot evidence = CacheEvidence.from(store.reportDetail(run.id()), Map.of(), true);
      assertEquals(2, evidence.units().size());
      assertTrue(CacheInspector.describe(evidence.units().get(0), evidence).contains("Cache lido: 0 tokens"));
      assertTrue(CacheInspector.describe(evidence.units().get(1), evidence)
          .contains("Zero não é presumido"));
    }
  }

  private static void record(Store store, String id, ObjectMapper json, String raw) throws Exception {
    store.recordEvent(id, json.readTree(raw), raw);
  }
}

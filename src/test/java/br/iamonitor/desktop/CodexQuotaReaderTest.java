package br.iamonitor.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CodexQuotaReaderTest {
  @Test void consultaJsonRpcSemIniciarTurno() throws Exception {
    Path javaBin = Path.of(System.getProperty("java.home"), "bin",
        System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java");
    CodexQuotaReader reader = new CodexQuotaReader(List.of(javaBin.toString(), "-cp",
        System.getProperty("java.class.path"), FixtureQuota.class.getName()));
    List<Store.QuotaSnapshot> snapshots = reader.read("before");
    assertEquals(1, snapshots.size());
    assertEquals("codex", snapshots.getFirst().limitId());
    assertEquals(25, snapshots.getFirst().usedPercent());
  }

  @Test void comparaApenasMesmaJanela() throws Exception {
    String now = "2026-10-11T10:00:00Z";
    Store.QuotaSnapshot before = new Store.QuotaSnapshot("before", "codex", "primary", 300,
        1800000000, 25, now, "pro");
    Store.QuotaSnapshot after = new Store.QuotaSnapshot("after", "codex", "primary", 300,
        1800000000, 31, now, "pro");
    assertTrue(QuotaPresentation.comparable(before, after));
    assertTrue(QuotaPresentation.describe("codex", List.of(before, after)).headline().contains("+6 p.p."));
    assertFalse(QuotaPresentation.comparable(before, new Store.QuotaSnapshot("after", "codex",
        "primary", 300, 1800000300, 5, now, "pro")));
    assertTrue(QuotaPresentation.describe("claude", List.of()).headline().contains("indisponível"));
  }

  @Test void guardaEApagaLeiturasDaSessao(@TempDir Path temp) throws Exception {
    Path project = java.nio.file.Files.createDirectory(temp.resolve("project"));
    try (Store store = new Store(temp.resolve("data"), new EventLogic())) {
      Store.Run run = store.begin(project, "Teste", "", "read-only");
      var snapshot = new Store.QuotaSnapshot("before", "codex", "primary", 300,
          1800000000, 25, "2026-10-11T10:00:00Z", "pro");
      store.saveQuotaSnapshots(run.id(), List.of(snapshot));
      assertEquals(1, store.quotaSnapshots(run.id()).size());
      store.restart(run.id());
      assertTrue(store.quotaSnapshots(run.id()).isEmpty());
      store.saveQuotaSnapshots(run.id(), List.of(snapshot));
      store.delete(run.id());
      assertTrue(store.quotaSnapshots(run.id()).isEmpty());
    }
  }

  @Test void aceitaRespostaComMultiplosLimites() throws Exception {
    var json = new ObjectMapper().readTree("""
        {"rateLimitsByLimitId":{"codex":{"primary":{"usedPercent":12.5,"windowDurationMins":300,
        "resetsAt":1800000000},"secondary":null},"codex_other":{"primary":{
        "usedPercent":42,"windowDurationMins":10080,"resetsAt":1800600000}}}}
        """);
    assertEquals(2, CodexQuotaReader.parse(json, "after", "2026-10-11T10:00:00Z").size());
  }
}

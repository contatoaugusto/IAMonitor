package br.iamonitor.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExecutionNarrativeTest {
  private static Store.Run run(String status, String error) {
    return new Store.Run("id", "C:/project", "pedido", null, "read-only", "codex", status,
        Instant.now().minusSeconds(10).toString(), null, null, error, 0, 0, 0, 0, 0, 0, 0, 0);
  }

  @Test void distingueConexaoOperacaoEConclusao() {
    var starting = ExecutionNarrative.describe(run("running", null), List.of(), 0, true);
    assertEquals("INICIANDO", starting.phase());
    assertTrue(starting.working());
    var event = new Store.Event(1, Instant.now().toString(), "item.completed", "local",
        "Arquivo concluído", "src/main.java");
    var active = ExecutionNarrative.describe(run("running", null), List.of(event), 1, true);
    assertEquals("OPERAÇÃO LOCAL", active.phase());
    assertEquals("Arquivo concluído", active.headline());
    assertEquals("src/main.java", active.detail());
    assertTrue(active.meta().contains("1 arquivos identificados"));
    var stale = new Store.Event(2, Instant.now().minusSeconds(8).toString(), "item.completed",
        "local", "Comando local concluído", "Get-Content README.md");
    var waiting = ExecutionNarrative.describe(run("running", null), List.of(stale), 1, true);
    assertEquals("AGUARDANDO EVENTO", waiting.phase());
    assertTrue(waiting.detail().contains("Último evento"));
    var completed = ExecutionNarrative.describe(run("completed", null), List.of(event), 1, false);
    assertEquals("CONCLUÍDA", completed.phase());
    assertFalse(completed.working());
    var failed = ExecutionNarrative.describe(run("failed", "A CLI falhou"), List.of(event), 1, false);
    assertEquals("A CLI falhou", failed.detail());
  }
}

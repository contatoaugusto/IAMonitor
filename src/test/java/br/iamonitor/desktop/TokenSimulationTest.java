package br.iamonitor.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TokenSimulationTest {
  private static Store.Detail detail(long input, long cache, long output, long total,
      boolean auditable) {
    Store.Run run = new Store.Run("id", "C:/project", "pedido", null, "read-only",
        "codex", "completed", "2026-10-09T00:00:00Z", null, 0, null,
        input, cache, output, 0, total, 0, 0, 0);
    return new Store.Detail(run, List.of(), List.of(), List.of(), Map.of(
        "skill", new EventLogic.Source(250L, 2),
        "agent", new EventLogic.Source(100L, 1),
        "spec", new EventLogic.Source(75L, 1),
        "hook", new EventLogic.Source(25L, 1)), auditable);
  }

  @Test void excluiSomenteOrigensEscolhidasSemAlterarContagemReal() {
    Store.Detail detail = detail(1200, 300, 450, 1650, true);
    var original = TokenSimulation.calculate(detail, Set.of());
    assertTrue(original.available());
    assertEquals(1650, original.reported());
    assertEquals(1650, original.scenario());
    var scenario = TokenSimulation.calculate(detail, Set.of("skill", "agent"));
    assertEquals(1650, scenario.reported());
    assertEquals(1300, scenario.scenario());
    assertEquals(850, scenario.input());
    assertEquals(350, scenario.savings());
    assertEquals(1200, detail.run().inputTokens());
  }

  @Test void limitaDescontoAEntradaSemCache() {
    var scenario = TokenSimulation.calculate(detail(350, 300, 100, 450, true),
        Set.of("skill", "agent", "spec", "hook"));
    assertEquals(450, scenario.requested());
    assertEquals(50, scenario.savings());
    assertEquals(400, scenario.scenario());
    assertEquals(300, scenario.input());
  }

  @Test void naoSimulaTotalSemEventosAuditaveis() {
    assertFalse(TokenSimulation.calculate(detail(1200, 300, 450, 1650, false),
        Set.of("skill")).available());
  }
}
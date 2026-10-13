package br.iamonitor.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class UsageGuideTest {
  private static Store.Run run(String status, long input, long cache, long output,
      long total, long missing) {
    return new Store.Run("id", "C:/project", "pedido", null, "read-only", "codex", status,
        "2026-10-09T00:00:00Z", null, null, null, input, cache, output, 0,
        total, missing, 0, 0);
  }

  @Test void usaSomenteContagensReportadasParaAsEtapas() {
    Store.Run session = run("completed", 1200, 300, 450, 1650, 0);
    var assessment = TokenAccounting.assess(session, true, true);
    var numbers = UsageGuide.JourneyCounts.from(session, assessment, true);
    assertTrue(numbers.available());
    assertEquals("1.200", numbers.input());
    assertEquals("300", numbers.cache());
    assertEquals("450", numbers.output());
    assertEquals("1.650", numbers.total());
    assertEquals("FINAL", numbers.status());
  }

  @Test void identificaCartoesCacheZonasEBarrasComEscala() {
    var counts = new UsageGuide.JourneyCounts("1.200", "300", "450", "1.650", "FINAL", true);
    List<Integer> clicked = new ArrayList<>();
    var journey = new UsageGuide.JourneyDiagram("Codex", counts, clicked::add);
    journey.setSize(495, 260);
    assertEquals(1, journey.stageAt(50, 50));
    assertEquals(3, journey.stageAt(300, 50));
    assertEquals(4, journey.stageAt(405, 55));
    assertEquals(9, journey.stageAt(400, 125));
    assertEquals(8, journey.stageAt(50, 180));
    assertEquals(5, journey.stageAt(405, 180));
    assertEquals(0, journey.stageAt(245, 15));
    journey.dispatchEvent(new MouseEvent(journey, MouseEvent.MOUSE_CLICKED,
        System.currentTimeMillis(), 0, 400, 125, 1, false));
    assertEquals(List.of(9), clicked);
    var flow = new UsageGuide.FlowDiagram("Codex", 1200, 450);
    flow.setSize(495, 169);
    assertEquals(2, flow.stageAt(50, 40));
    assertEquals(4, flow.stageAt(250, 40));
    assertEquals(5, flow.stageAt(420, 40));
    assertEquals(4, flow.stageAt(250, 135));
    assertEquals(6, flow.stageAt(250, 151));
  }
  @Test void naoExibeContagemHistoricaSemEventosConferiveis() {
    Store.Run session = run("completed", 1200, 300, 450, 1650, 0);
    var assessment = TokenAccounting.assess(session, true, false);
    var numbers = UsageGuide.JourneyCounts.from(session, assessment, false);
    assertFalse(numbers.available());
    assertEquals("—", numbers.input());
    assertEquals("—", numbers.cache());
    assertEquals("—", numbers.output());
    assertEquals("—", numbers.total());
  }
}

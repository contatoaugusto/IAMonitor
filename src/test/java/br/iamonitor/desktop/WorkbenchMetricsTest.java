package br.iamonitor.desktop;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WorkbenchMetricsTest {
  @Test void separaUsoReportadoDeEstimativasPorCategoria() {
    Store.Run run = new Store.Run("12345678-1234-1234-1234-123456789abc", "C:\\projeto",
        "pedido", "modelo", "workspace-write", "codex", "completed",
        "2026-10-11T10:00:00Z", "2026-10-11T10:01:00Z", 0, null,
        1200, 300, 450, 0, 1650, 1, 0, 0);
    List<Store.Event> events = List.of(
        new Store.Event(1, "2026-10-11T10:00:00Z", "turn.started", "model", "Início", ""),
        new Store.Event(2, "2026-10-11T10:00:30Z", "turn.completed", "model", "Fim", "",
            new EventLogic.Usage(1200, 300, 450, 0, 1650)),
        new Store.Event(3, "2026-10-11T10:00:40Z", "turn.started", "model", "Início", ""),
        new Store.Event(4, "2026-10-11T10:01:00Z", "turn.completed", "model", "Fim", ""));
    List<Store.Artifact> files = List.of(
        new Store.Artifact("a", "Spec", "", 100, 25, ""),
        new Store.Artifact("b", "Spec", "", 100, 35, ""),
        new Store.Artifact("c", "Skill", "", 100, 15, ""));
    var detail = new Store.Detail(run, events, files, List.of(), Map.of(), true);
    var snapshot = WorkbenchMetrics.from(detail);
    assertEquals(2, snapshot.completed());
    assertEquals(1, snapshot.withoutUsage());
    assertEquals(1650, snapshot.cycles().getFirst().usage().total());
    assertNull(snapshot.cycles().getLast().usage());
    assertEquals(EventLogic.ARTIFACT_CATEGORIES.size(), snapshot.categories().size());
    assertEquals(new WorkbenchMetrics.Category("Skill", 1, 15), snapshot.categories().getFirst());
    assertEquals(new WorkbenchMetrics.Category("Command", 0, 0), snapshot.categories().get(2));
    assertEquals(new WorkbenchMetrics.Category("Spec", 2, 60), snapshot.categories().get(3));
    assertEquals(new WorkbenchMetrics.Category("Arquivo", 0, 0), snapshot.categories().getLast());
  }

  @Test void mostraTodoCatalogoMesmoSemArquivos() {
    WorkbenchMetrics.Snapshot empty = WorkbenchMetrics.from(null);
    assertEquals(EventLogic.ARTIFACT_CATEGORIES.size(), empty.categories().size());
    assertTrue(empty.categories().stream().allMatch(category -> category.files() == 0
        && category.estimatedTokens() == 0));
  }
}

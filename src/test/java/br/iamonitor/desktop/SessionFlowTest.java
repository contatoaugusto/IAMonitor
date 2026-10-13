package br.iamonitor.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.GraphicsEnvironment;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SessionFlowTest {
  private final ObjectMapper json = new ObjectMapper();

  @Test void separaDoisTurnosReportadosEUmPendente() {
    var first = new EventLogic.Usage(120, 20, 30, 0, 150);
    var second = new EventLogic.Usage(80, 0, 25, 0, 105);
    var events = List.of(
        event(1, "turn.started", null),
        message(2),
        event(3, "turn.completed", first),
        event(4, "turn.started", null),
        message(5),
        message(6),
        event(7, "turn.completed", second),
        event(8, "turn.started", null));
    var flow = SessionFlow.from(events, "codex", true);
    assertEquals("turno", flow.unit());
    assertEquals(2, flow.completed());
    assertEquals(2, flow.measured());
    assertEquals(1, flow.pending());
    assertEquals(3, flow.cycles().size());
    assertEquals(3, flow.messageEvents());
    assertEquals(1, flow.cycles().getFirst().messageEvents());
    assertEquals(2, flow.cycles().get(1).messageEvents());
    assertEquals(120, flow.cycles().getFirst().usage().input());
    assertEquals(25, flow.cycles().get(1).usage().output());
    assertFalse(flow.cycles().get(2).completed());
  }

  @Test void resultadoAgregadoNaoViraContagemDeRequisicoes() {
    var flow = SessionFlow.from(List.of(event(1, "turn.completed", null)), "claude", true);
    assertEquals("resultado de uso", flow.unit());
    assertEquals(1, flow.completed());
    assertEquals(0, flow.measured());
    assertEquals(1, flow.withoutUsage());
    assertEquals(1, flow.cycles().size());
  }

  @Test void consultaTodosOsTurnosMesmoQuandoListaRecenteFoiTruncada(@TempDir Path temp)
      throws Exception {
    Path project = Files.createDirectory(temp.resolve("project"));
    try (Store store = new Store(temp.resolve("data"), new EventLogic())) {
      String id = store.begin(project, "Pedido", "", "read-only").id();
      record(store, id, "{\"type\":\"turn.completed\",\"usage\":{\"input_tokens\":120,\"output_tokens\":30,\"total_tokens\":150}}");
      for (int index = 0; index < 501; index++)
        record(store, id, "{\"type\":\"item.updated\",\"item\":{\"type\":\"reasoning\"}}");
      record(store, id, "{\"type\":\"turn.completed\",\"usage\":{\"input_tokens\":80,\"output_tokens\":20,\"total_tokens\":100}}");
      Store.Detail detail = store.detail(id);
      assertTrue(detail.events().stream().noneMatch(event -> event.usage() != null
          && event.usage().input() == 120));
      var flowEvents = store.flowEvents(id);
      assertEquals(2, flowEvents.size());
      var flow = SessionFlow.from(flowEvents, "codex", true);
      assertEquals(2, flow.completed());
      assertEquals(200, flow.cycles().stream().mapToLong(cycle -> cycle.usage().input()).sum());
      assertEquals(250, detail.run().totalTokens());
      String stage = StageDetails.describe(store, detail, 4);
      assertTrue(stage.contains("Entrada 120"));
      assertTrue(stage.contains("Entrada 80"));
    }
  }

  @Test void previsualizaPercursosRepetidos() throws Exception {
    Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
    var flow = SessionFlow.from(List.of(
        event(1, "turn.started", null), message(2),
        event(3, "turn.completed", new EventLogic.Usage(120, 20, 30, 0, 150)),
        event(4, "turn.started", null), message(5), message(6),
        event(7, "turn.completed", new EventLogic.Usage(90, 10, 25, 0, 115))),
        "codex", true);
    SwingUtilities.invokeAndWait(() -> {
      JFrame frame = new JFrame("Prévia dos percursos");
      try {
        List<Integer> cacheSelections = new java.util.ArrayList<>();
        JPanel content = SessionFlowPanel.create(flow, cacheSelections::add);
        frame.setContentPane(new JScrollPane(content));
        frame.setSize(1020, 590);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        JLabel cache = findCacheLabel(content);
        assertTrue(cache != null);
        cache.dispatchEvent(new MouseEvent(cache, MouseEvent.MOUSE_CLICKED,
            System.currentTimeMillis(), 0, 10, 5, 1, false));
        assertEquals(List.of(1), cacheSelections);
        BufferedImage preview = new BufferedImage(frame.getWidth(), frame.getHeight(),
            BufferedImage.TYPE_INT_RGB);
        frame.printAll(preview.createGraphics());
        ImageIO.write(preview, "png", Path.of("target/session-flow-multiple-preview.png").toFile());
      } catch (Exception error) {
        throw new IllegalStateException(error);
      } finally {
        frame.dispose();
      }
    });
    assertTrue(Files.size(Path.of("target/session-flow-multiple-preview.png")) > 10000);
  }
  private static JLabel findCacheLabel(Container parent) {
    for (Component child : parent.getComponents()) {
      if (child instanceof JLabel label && label.getText().contains("tokens de cache")) return label;
      if (child instanceof Container container) {
        JLabel found = findCacheLabel(container);
        if (found != null) return found;
      }
    }
    return null;
  }
  private static Store.Event event(int seq, String type, EventLogic.Usage usage) {
    return new Store.Event(seq, "2026-10-09T16:00:00Z", type, "model", "", "", usage);
  }

  private static Store.Event message(int seq) {
    return new Store.Event(seq, "2026-10-09T16:00:00Z", "item.completed",
        "message", "Mensagem", "Texto", null);
  }

  private void record(Store store, String id, String payload) throws Exception {
    var event = json.readTree(payload);
    store.recordEvent(id, event, json.writeValueAsString(event));
  }
}

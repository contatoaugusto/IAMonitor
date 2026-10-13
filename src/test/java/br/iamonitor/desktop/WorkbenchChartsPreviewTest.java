package br.iamonitor.desktop;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.Color;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

class WorkbenchChartsPreviewTest {
  private static WorkbenchMetrics.Snapshot snapshot() {
    List<SessionFlow.Cycle> cycles = List.of(
        new SessionFlow.Cycle(1, "2026-10-11T12:00:00Z",
            new EventLogic.Usage(500, 100, 200, 0, 740), true, 1),
        new SessionFlow.Cycle(2, "2026-10-11T12:01:00Z",
            new EventLogic.Usage(4709555, 4635904, 15200, 100, 4724755), true, 2),
        new SessionFlow.Cycle(3, "2026-10-11T12:02:00Z", null, false, 0));
    List<WorkbenchMetrics.Category> categories = EventLogic.ARTIFACT_CATEGORIES.stream()
        .map(name -> switch (name) {
          case "Skill" -> new WorkbenchMetrics.Category(name, 2, 850);
          case "Command" -> new WorkbenchMetrics.Category(name, 1, 230);
          case "Spec" -> new WorkbenchMetrics.Category(name, 1, 520);
          default -> new WorkbenchMetrics.Category(name, 0, 0);
        }).toList();
    return new WorkbenchMetrics.Snapshot(cycles, categories, 2, 0, 1, true);
  }

  @Test void criaUmComponenteGraficoPorCicloInclusivePendente() {
    WorkbenchCharts.Usage chart = new WorkbenchCharts.Usage();
    chart.setData(snapshot());
    assertEquals(3, chart.cardCount());
  }

  @Test void exibeSomenteParcelasSomaveisDeCadaCiclo() {
    EventLogic.Usage usage = new EventLogic.Usage(1200, 300, 450, 100, 1700);
    List<WorkbenchCharts.CycleCard.Metric> metrics = WorkbenchCharts.CycleCard.metrics(usage);
    assertEquals(List.of("Entrada sem cache", "Cache", "Saída", "Outros"),
        metrics.stream().map(WorkbenchCharts.CycleCard.Metric::label).toList());
    assertEquals(List.of(900L, 300L, 450L, 50L),
        metrics.stream().map(WorkbenchCharts.CycleCard.Metric::tokens).toList());
    assertEquals("900 tokens", WorkbenchCharts.CycleCard.value(metrics.get(0)));
    assertEquals("300 tokens", WorkbenchCharts.CycleCard.value(metrics.get(1)));
    assertEquals(usage.total(), metrics.stream().mapToLong(WorkbenchCharts.CycleCard.Metric::tokens).sum());
    assertEquals(List.of("Entrada sem cache", "Cache", "Saída"), WorkbenchCharts.CycleCard.metrics(
        new EventLogic.Usage(1200, 0, 450, 0, 1650)).stream()
        .map(WorkbenchCharts.CycleCard.Metric::label).toList());
    assertTrue(WorkbenchCharts.CycleCard.metrics(null).isEmpty());
    assertTrue(WorkbenchCharts.CycleCard.metrics(new EventLogic.Usage(100, 101, 20, 0, 120)).isEmpty());
  }

  @Test void renderizaGraficosComCategoriasVazias() throws Exception {
    Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
    JFrame[] frame = new JFrame[1];
    SwingUtilities.invokeAndWait(() -> {
      JPanel content = new JPanel(new GridLayout(1, 2, 10, 0));
      content.setBackground(new Color(16, 36, 49));
      WorkbenchCharts.Usage usage = new WorkbenchCharts.Usage();
      WorkbenchCharts.Artifacts artifacts = new WorkbenchCharts.Artifacts();
      usage.setData(snapshot());
      artifacts.setData(snapshot());
      List<Integer> selectedCache = new java.util.ArrayList<>();
      usage.onCacheSelected(selectedCache::add);
      List<String> selected = new java.util.ArrayList<>();
      artifacts.onCategorySelected(selected::add);
      content.add(usage);
      content.add(artifacts);
      frame[0] = new JFrame("Prévia dos gráficos");
      frame[0].setContentPane(content);
      frame[0].setSize(1100, 500);
      frame[0].setVisible(true);
      JScrollPane cycleScroll = (JScrollPane) usage.getComponent(1);
      JPanel cycleList = (JPanel) cycleScroll.getViewport().getView();
      WorkbenchCharts.CycleCard second = (WorkbenchCharts.CycleCard) cycleList.getComponent(1);
      second.dispatchEvent(new MouseEvent(second, MouseEvent.MOUSE_CLICKED,
          System.currentTimeMillis(), 0, 40, 90, 1, false));
      assertEquals(List.of(2), selectedCache);
      JScrollPane artifactScroll = (JScrollPane) artifacts.getComponent(1);
      JPanel canvas = (JPanel) artifactScroll.getViewport().getView();
      assertTrue(canvas.getWidth() >= 460);
      MouseEvent first = new MouseEvent(canvas, MouseEvent.MOUSE_CLICKED,
          System.currentTimeMillis(), 0, 20, 15, 1, false);
      assertTrue(canvas.getToolTipText(new MouseEvent(canvas, MouseEvent.MOUSE_MOVED,
          System.currentTimeMillis(), 0, 20, 15, 0, false)).contains("Skill: 2 arquivos"));
      canvas.dispatchEvent(first);
      assertEquals(List.of("Skill"), selected);
      assertTrue(canvas.getToolTipText(new MouseEvent(canvas, MouseEvent.MOUSE_MOVED,
          System.currentTimeMillis(), 0, 20, 63, 0, false)).contains("Agent: 0 arquivos"));
      canvas.dispatchEvent(new MouseEvent(canvas, MouseEvent.MOUSE_CLICKED,
          System.currentTimeMillis(), 0, 20, 63, 1, false));
      assertEquals(List.of("Skill", "Agent"), selected);
      try {
        BufferedImage image = new BufferedImage(content.getWidth(), content.getHeight(), BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        content.printAll(graphics);
        graphics.dispose();
        ImageIO.write(image, "png", Path.of("target/workbench-charts-preview.png").toFile());
        frame[0].setSize(820, 300);
        frame[0].validate();
        BufferedImage compact = new BufferedImage(content.getWidth(), content.getHeight(), BufferedImage.TYPE_INT_RGB);
        var compactGraphics = compact.createGraphics();
        content.printAll(compactGraphics);
        compactGraphics.dispose();
        ImageIO.write(compact, "png", Path.of("target/workbench-charts-compact-preview.png").toFile());
      } catch (Exception error) { throw new RuntimeException(error); }
      frame[0].dispose();
    });
    assertTrue(Files.size(Path.of("target/workbench-charts-preview.png")) > 10000);
    assertTrue(Files.size(Path.of("target/workbench-charts-compact-preview.png")) > 10000);
  }
}

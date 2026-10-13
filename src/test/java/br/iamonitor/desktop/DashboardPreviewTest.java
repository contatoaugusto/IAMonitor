package br.iamonitor.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.event.MouseEvent;
import java.util.List;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DashboardPreviewTest {
  @Test void renderizaPainelEFormularioCentralizado(@TempDir Path temp) throws Exception {
    Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    Path project = Files.createDirectory(temp.resolve("project"));
    Files.writeString(project.resolve("AGENTS.md"), "Instruções do projeto.");
    Path skill = project.resolve("skills/demo/SKILL.md");
    Path spec = project.resolve("specs/plano.md");
    Path hook = project.resolve("hooks/verificar.ps1");
    for (Path file : List.of(skill, spec, hook)) Files.createDirectories(file.getParent());
    Files.writeString(skill, "# Skill de demonstração\nLeia os eventos.");
    Files.writeString(spec, "# Plano\nCriar o painel.");
    Files.writeString(hook, "Write-Output 'verificado'");
    Store store = new Store(temp.resolve("data"), new EventLogic());
    Store.Run run = store.begin(project, "Crie um painel com métricas para esta sessão.", "", "workspace-write");
    store.observeArtifacts(run.id(), "\"" + project.resolve("AGENTS.md") + "\" \""
        + skill + "\" \"" + spec + "\" \"" + hook + "\"", project);
    var mapper = new ObjectMapper();
    var event = mapper.readTree("{\"type\":\"turn.completed\",\"usage\":{\"input_tokens\":1200,\"cached_input_tokens\":300,\"output_tokens\":450,\"reasoning_tokens\":150,\"total_tokens\":1650}}");
    store.recordEvent(run.id(), event, mapper.writeValueAsString(event));
    long reset = java.time.Instant.now().plusSeconds(3600).getEpochSecond();
    store.saveQuotaSnapshots(run.id(), List.of(
        new Store.QuotaSnapshot("before", "codex", "primary", 300, reset, 25,
            java.time.Instant.now().minusSeconds(2).toString(), "pro"),
        new Store.QuotaSnapshot("after", "codex", "primary", 300, reset, 31,
            java.time.Instant.now().toString(), "pro")));
    store.sample(run.id(), 12.5, 425000000);
    store.finish(run.id(), "completed", 0, null);
    Path javaBin = Path.of(System.getProperty("java.home"), "bin",
        System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java");
    List<String> command = List.of(javaBin.toString(), "-cp", System.getProperty("java.class.path"),
        FixtureCodex.class.getName());
    Dashboard[] holder = new Dashboard[1];
    SwingUtilities.invokeAndWait(() -> { holder[0] = new Dashboard(store, command); holder[0].setVisible(true); });
    try {
      Thread.sleep(1800);
      SwingUtilities.invokeAndWait(() -> save(holder[0], Path.of("target/dashboard-preview.png")));
      assertTrue(Files.size(Path.of("target/dashboard-preview.png")) > 10000);
      assertEquals("Ajuda", holder[0].getJMenuBar().getMenu(0).getText());
      assertEquals("Sobre o IAMonitor...", holder[0].getJMenuBar().getMenu(0).getItem(2).getText());
      assertNotNull(findButton(holder[0], "Mais ações ▾"));
      SwingUtilities.invokeAndWait(() -> {
        findButton(holder[0], "Apresentar").doClick();
        assertEquals("Sair da apresentação", findButton(holder[0], "Sair da apresentação").getText());
        assertTrue(!findButton(holder[0], "+ Nova sessão").isShowing());
        save(holder[0], Path.of("target/presentation-preview.png"));
        findButton(holder[0], "Sair da apresentação").doClick();
        assertTrue(findButton(holder[0], "+ Nova sessão").isShowing());
      });
      assertTrue(Files.size(Path.of("target/presentation-preview.png")) > 10000);
      SwingUtilities.invokeAndWait(() -> {
        JLabel equation = findLabel(holder[0], "900 entrada sem cache + 300 cache + 450 saída = 1.650 tokens");
        assertNotNull(equation);
        assertTrue(equation.getHeight() > 0);
        JButton quotaButton = findButton(holder[0], "Consultar fonte oficial");
        assertNotNull(quotaButton);
        assertTrue(quotaButton.isShowing());
        assertEquals(quotaButton.getHeight(), quotaButton.getVisibleRect().height);

        assertNotNull(findUsage(holder[0]));
        assertTrue(findUsage(holder[0]).getHeight() >= 250);
        JTabbedPane tabs = findTabs(holder[0]);
        assertNotNull(tabs);
        assertEquals("Visão geral", tabs.getTitleAt(0));
        assertEquals("Explorar dados", tabs.getTitleAt(1));
        assertNotNull(findLabel(holder[0], "VISÃO GERAL DA SESSÃO"));
        assertNotNull(findLabel(holder[0], "OPERAÇÃO ATUAL E REGISTRO RECENTE"));
      });
      SwingUtilities.invokeAndWait(() -> {
        for (String name : List.of("Relatório PDF", "Apresentar", "Mais ações ▾")) {
          JButton button = findButton(holder[0], name);
          assertNotNull(button);
          assertTrue(button.isShowing());
        }
        findTabs(holder[0]).setSelectedIndex(1);
        assertNotNull(findButton(holder[0], "Dados e envio"));
        save(holder[0], Path.of("target/explore-data-preview.png"));
        assertNotNull(findLabel(holder[0], "SIMULE A ECONOMIA · CLIQUE EM RETIRAR"));
        JTable origins = findTable(holder[0], "Origem", "Tokens", "Base");
        assertNotNull(origins);
        var last = origins.getCellRect(origins.getRowCount() - 1, 0, true);
        assertTrue(last.y + last.height <= origins.getVisibleRect().height);
      });
      JToggleButton[] controls = {
          findToggle(holder[0], "Skills"), findToggle(holder[0], "Agents"),
          findToggle(holder[0], "Specs"), findToggle(holder[0], "Hooks")
      };
      long estimated = 0;
      for (String key : List.of("skill", "agent", "spec", "hook"))
        estimated += store.detail(run.id()).inputSources().get(key).estimatedTokens();
      long expectedScenario = 1650 - Math.min(estimated, 900);
      SwingUtilities.invokeAndWait(() -> {
        for (JToggleButton control : controls) {
          assertNotNull(control);
          assertTrue(control.isEnabled());
          assertTrue(control.getText().startsWith("Retirar "));
          control.doClick();
          assertTrue(!control.isSelected());
          assertTrue(control.getText().startsWith("Incluir "));
        }
        assertNotNull(findLabel(holder[0], "≈ " + java.text.NumberFormat.getIntegerInstance(
            java.util.Locale.forLanguageTag("pt-BR")).format(expectedScenario)));
        JTable origins = findTable(holder[0], "Origem", "Tokens", "Base");
        for (int row = 0; row < origins.getRowCount(); row++) {
          if (List.of("skill", "agent", "spec", "hook").contains(origins.getValueAt(row, 0)))
            assertEquals("0", origins.getValueAt(row, 1));
        }
        assertNotNull(findLabel(holder[0], "1.650"));
        save(holder[0], Path.of("target/simulation-preview.png"));
        for (JToggleButton control : controls) {
          control.doClick();
          assertTrue(control.isSelected());
          assertTrue(control.getText().startsWith("Retirar "));
        }
        assertNotNull(findLabel(holder[0], "≈ 1.650"));
      });
      assertTrue(Files.size(Path.of("target/simulation-preview.png")) > 10000);
      SwingUtilities.invokeLater(() -> holder[0].getJMenuBar().getMenu(0).getItem(0).doClick());
      JDialog guide = null;
      for (int i = 0; i < 50 && guide == null; i++) {
        Thread.sleep(100);
        for (Window window : Window.getWindows()) {
          if (window instanceof JDialog candidate && candidate.isShowing()
              && "Como são medidos os tokens".equals(candidate.getTitle())) guide = candidate;
        }
      }
      assertNotNull(guide);
      JDialog shownGuide = guide;
      SwingUtilities.invokeAndWait(() -> {
        assertTrue(holder[0].getGlassPane().isVisible());
        save(holder[0], Path.of("target/main-dimmed-preview.png"));
        assertNotNull(findFlowDiagram(shownGuide));
        UsageGuide.JourneyDiagram journey = findJourneyDiagram(shownGuide);
        assertNotNull(journey);
        assertTrue(journey.getHeight() >= 400);
        assertEquals("1.200", journey.counts().input());
        assertEquals("300", journey.counts().cache());
        assertEquals("450", journey.counts().output());
        assertEquals("1.650", journey.counts().total());
        assertNotNull(findLabel(shownGuide, "Como são medidos os tokens"));
        assertNotNull(findLabel(shownGuide, "1 turno concluído"));
        save(shownGuide, Path.of("target/usage-guide-preview.png"));
        JScrollPane guideScroll = findScroll(shownGuide);
        assertNotNull(guideScroll);
        guideScroll.getVerticalScrollBar().setValue(520);
        save(shownGuide, Path.of("target/usage-guide-middle-preview.png"));
        guideScroll.getVerticalScrollBar().setValue(guideScroll.getVerticalScrollBar().getMaximum());
        save(shownGuide, Path.of("target/usage-guide-bottom-preview.png"));
        guideScroll.getVerticalScrollBar().setValue(0);
        JTabbedPane guideTabs = findTabs(shownGuide);
        assertNotNull(guideTabs);
        assertEquals(2, guideTabs.getTabCount());
        assertEquals("Como cada IA conta", guideTabs.getTitleAt(1));
        guideTabs.setSelectedIndex(1);
        ((JScrollPane) guideTabs.getComponentAt(1)).getVerticalScrollBar().setValue(0);
        save(shownGuide, Path.of("target/token-methodology-top-preview.png"));
        JTextArea simulated = findEditableTextArea(shownGuide);
        assertNotNull(simulated);
        simulated.setText("Olá, este é um exemplo de prompt para comparar.");
        JButton calculate = findButton(shownGuide, "Calcular tokens");
        assertNotNull(calculate);
        calculate.doClick();
        JScrollPane methodScroll = (JScrollPane) guideTabs.getComponentAt(1);
        shownGuide.validate();
        methodScroll.getVerticalScrollBar().setValue(methodScroll.getVerticalScrollBar().getMaximum());
        save(shownGuide, Path.of("target/token-methodology-preview.png"));
        guideTabs.setSelectedIndex(0);
      });
      UsageGuide.JourneyDiagram journey = findJourneyDiagram(shownGuide);
      SwingUtilities.invokeLater(() -> journey.dispatchEvent(new MouseEvent(journey,
          MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0,
          journey.getWidth() * 600 / 990, journey.getHeight() * 100 / 520, 1, false)));
      JDialog stageDialog = null;
      for (int i = 0; i < 50 && stageDialog == null; i++) {
        Thread.sleep(100);
        for (Window window : Window.getWindows()) {
          if (window instanceof JDialog candidate && candidate.isShowing()
              && "Detalhes da etapa: Contexto".equals(candidate.getTitle())) stageDialog = candidate;
        }
      }
      assertNotNull(stageDialog);
      JDialog openedStage = stageDialog;
      for (int i = 0; i < 50; i++) {
        Thread.sleep(100);
        JTextArea[] text = new JTextArea[1];
        SwingUtilities.invokeAndWait(() -> text[0] = findTextArea(openedStage));
        if (text[0] != null && text[0].getText().contains("AGENTS.md")) break;
      }
      SwingUtilities.invokeAndWait(() -> {
        assertTrue(findTextArea(openedStage).getText().contains("AGENTS.md"));
        assertTrue(findTextArea(openedStage).getText().contains("Instruções do projeto."));
        save(openedStage, Path.of("target/stage-details-preview.png"));
        openedStage.dispose();
      });
      SwingUtilities.invokeLater(() -> journey.dispatchEvent(new MouseEvent(journey,
          MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0,
          journey.getWidth() * 810 / 990, journey.getHeight() * 110 / 520, 1, false)));
      JDialog inputDialog = null;
      for (int i = 0; i < 50 && inputDialog == null; i++) {
        Thread.sleep(100);
        for (Window window : Window.getWindows()) {
          if (window instanceof JDialog candidate && candidate.isShowing()
              && "Detalhes da etapa: Envio ao modelo".equals(candidate.getTitle())) inputDialog = candidate;
        }
      }
      assertNotNull(inputDialog);
      JDialog openedInput = inputDialog;
      for (int i = 0; i < 50; i++) {
        Thread.sleep(100);
        JTextArea[] text = new JTextArea[1];
        SwingUtilities.invokeAndWait(() -> text[0] = findTextArea(openedInput));
        if (text[0] != null && text[0].getText().contains("1.200")) break;
      }
      SwingUtilities.invokeAndWait(() -> {
        assertTrue(findTextArea(openedInput).getText().contains("1.200"));
        assertTrue(findTextArea(openedInput).getText().contains("300"));
        openedInput.dispose();
        shownGuide.dispose();
      });
      assertTrue(Files.size(Path.of("target/stage-details-preview.png")) > 10000);
      assertTrue(Files.size(Path.of("target/usage-guide-preview.png")) > 10000);
      assertTrue(Files.size(Path.of("target/main-dimmed-preview.png")) > 10000);
      JButton newButton = findButton(holder[0], "+ Nova sessão");
      assertNotNull(newButton);
      SwingUtilities.invokeLater(newButton::doClick);
      JDialog dialog = null;
      for (int i = 0; i < 50 && dialog == null; i++) {
        Thread.sleep(100);
        for (Window window : Window.getWindows()) {
          if (window instanceof JDialog candidate && candidate.isShowing()
              && "Nova sessão de IA".equals(candidate.getTitle())) dialog = candidate;
        }
      }
      assertNotNull(dialog);
      JDialog opened = dialog;
      SwingUtilities.invokeAndWait(() -> {
        JTextArea connection = findTextArea(opened);
        assertNotNull(connection);
        assertTrue(connection.getText().contains("Não precisa entrar na pasta"));
        assertTrue(connection.getParent().getHeight() >= 150);
        assertTrue(Math.abs(opened.getBounds().getCenterX() - holder[0].getBounds().getCenterX()) < 30);
        assertTrue(Math.abs(opened.getBounds().getCenterY() - holder[0].getBounds().getCenterY()) < 30);
        JTextArea request = findEditableTextArea(opened);
        assertNotNull(request);
        JButton reuse = findButton(opened, "Reutilizar sessão selecionada");
        assertNotNull(reuse);
        assertTrue(reuse.isEnabled());
        reuse.doClick();
        assertEquals(run.prompt(), request.getText());
        assertNotNull(findLabel(opened,
            "Para comparar execuções, use cópias iguais do projeto: a primeira execução modifica a pasta."));
        save(opened, Path.of("target/new-session-preview.png"));
        request.setText("sleep Novo pedido para acompanhar");
        JButton execute = findButton(opened, "Executar e acompanhar");
        assertNotNull(execute);
        execute.doClick();
      });
      assertTrue(Files.size(Path.of("target/new-session-preview.png")) > 10000);
      Store.Run[] selected = new Store.Run[1];
      for (int i = 0; i < 80; i++) {
        Thread.sleep(100);
        SwingUtilities.invokeAndWait(() -> selected[0] = (Store.Run) findList(holder[0]).getSelectedValue());
        if (selected[0] != null && "sleep Novo pedido para acompanhar".equals(selected[0].prompt())) break;
      }
      assertNotNull(selected[0]);
      assertTrue("sleep Novo pedido para acompanhar".equals(selected[0].prompt()));
      assertTrue(!selected[0].id().equals(run.id()));
      Thread.sleep(650);
      SwingUtilities.invokeAndWait(() -> {
        assertTrue(!holder[0].getGlassPane().isVisible());
        assertEquals(0, findTabs(holder[0]).getSelectedIndex());
        save(holder[0], Path.of("target/execution-preview.png"));
        assertTrue(findUsage(holder[0]).getHeight() >= 150);
        assertNotNull(findLabel(holder[0], "OPERAÇÃO ATUAL E REGISTRO RECENTE"));
        assertTrue(findLabel(holder[0], "OPERAÇÃO LOCAL") != null || findLabel(holder[0], "MODELO DE IA") != null);
        assertTrue(findButton(holder[0], "Interromper").isShowing());
        assertTrue(!findToggle(holder[0], "Skills").isShowing());
        assertTrue(!findTable(holder[0], "Origem", "Tokens", "Base").isShowing());
      });
      assertTrue(Files.size(Path.of("target/execution-preview.png")) > 10000);
      for (int i = 0; i < 80 && !"completed".equals(store.run(selected[0].id()).status()); i++)
        Thread.sleep(100);
      assertEquals("completed", store.run(selected[0].id()).status());
      Thread.sleep(1800);
      SwingUtilities.invokeAndWait(() -> {
        findTabs(holder[0]).setSelectedIndex(1);
        assertNotNull(findButton(holder[0], "Dados e envio"));
        save(holder[0], Path.of("target/explore-data-preview.png"));
        assertTrue(findToggle(holder[0], "Skills").isShowing());
        assertTrue(findTable(holder[0], "Origem", "Tokens", "Base").isShowing());
      });

      JTable sources = findTable(holder[0], "Origem", "Tokens", "Base");
      assertNotNull(sources);
      SwingUtilities.invokeLater(() -> {
        var cell = sources.getCellRect(0, 0, true);
        sources.dispatchEvent(new MouseEvent(sources, MouseEvent.MOUSE_CLICKED,
            System.currentTimeMillis(), 0, cell.x + 8, cell.y + 8, 2, false));
      });
      JDialog sourceDialog = null;
      for (int i = 0; i < 50 && sourceDialog == null; i++) {
        Thread.sleep(100);
        for (Window window : Window.getWindows()) {
          if (window instanceof JDialog candidate && candidate.isShowing()
              && candidate.getTitle().startsWith("Detalhes da origem: Prompt")) sourceDialog = candidate;
        }
      }
      assertNotNull(sourceDialog);
      JDialog shown = sourceDialog;
      for (int i = 0; i < 50; i++) {
        Thread.sleep(100);
        JTextArea[] text = new JTextArea[1];
        SwingUtilities.invokeAndWait(() -> text[0] = findTextArea(shown));
        if (text[0] != null && text[0].getText().contains("Novo pedido para acompanhar")) break;
      }
      SwingUtilities.invokeAndWait(() -> {
        assertTrue(findTextArea(shown).getText().contains("Novo pedido para acompanhar"));
        save(shown, Path.of("target/source-details-preview.png"));
        shown.dispose();
      });
      assertTrue(Files.size(Path.of("target/source-details-preview.png")) > 10000);
      SwingUtilities.invokeLater(() -> {
        for (int row = 0; row < sources.getRowCount(); row++) {
          if (!"cache".equals(sources.getValueAt(row, 0))) continue;
          var cell = sources.getCellRect(row, 0, true);
          sources.dispatchEvent(new MouseEvent(sources, MouseEvent.MOUSE_CLICKED,
              System.currentTimeMillis(), 0, cell.x + 8, cell.y + 8, 1, false));
          break;
        }
      });
      JDialog cacheDialog = null;
      for (int i = 0; i < 50 && cacheDialog == null; i++) {
        Thread.sleep(100);
        for (Window window : Window.getWindows())
          if (window instanceof JDialog candidate && candidate.isShowing()
              && "Inspeção do cache".equals(candidate.getTitle())) cacheDialog = candidate;
      }
      assertNotNull(cacheDialog);
      JDialog cacheShown = cacheDialog;
      for (int i = 0; i < 50 && findLabel(cacheShown, "CACHE DE ENTRADA · OpenAI Codex") == null; i++)
        Thread.sleep(100);
      SwingUtilities.invokeAndWait(() -> {
        assertNotNull(findLabel(cacheShown, "CACHE DE ENTRADA · OpenAI Codex"));
        save(cacheShown, Path.of("target/cache-from-dashboard-preview.png"));
        cacheShown.dispose();
      });
      assertTrue(Files.size(Path.of("target/cache-from-dashboard-preview.png")) > 10000);
    } finally {
      SwingUtilities.invokeAndWait(() -> holder[0].dispatchEvent(
          new java.awt.event.WindowEvent(holder[0], java.awt.event.WindowEvent.WINDOW_CLOSING)));
    }
  }

  private static JScrollPane findScroll(Container parent) {
    for (Component child : parent.getComponents()) {
      if (child instanceof JScrollPane scroll) return scroll;
      if (child instanceof Container nested) {
        JScrollPane match = findScroll(nested);
        if (match != null) return match;
      }
    }
    return null;
  }

  private static WorkbenchCharts.Usage findUsage(Container parent) {
    for (Component child : parent.getComponents()) {
      if (child instanceof WorkbenchCharts.Usage usage) return usage;
      if (child instanceof Container nested) {
        WorkbenchCharts.Usage match = findUsage(nested);
        if (match != null) return match;
      }
    }
    return null;
  }

  private static UsageGuide.JourneyDiagram findJourneyDiagram(Container parent) {
    for (Component child : parent.getComponents()) {
      if (child instanceof UsageGuide.JourneyDiagram journey) return journey;
      if (child instanceof Container nested) {
        UsageGuide.JourneyDiagram match = findJourneyDiagram(nested);
        if (match != null) return match;
      }
    }
    return null;
  }

  private static UsageGuide.FlowDiagram findFlowDiagram(Container parent) {
    for (Component child : parent.getComponents()) {
      if (child instanceof UsageGuide.FlowDiagram flow) return flow;
      if (child instanceof Container nested) {
        UsageGuide.FlowDiagram match = findFlowDiagram(nested);
        if (match != null) return match;
      }
    }
    return null;
  }

  private static JLabel findLabel(Container parent, String text) {
    for (Component child : parent.getComponents()) {
      if (child instanceof JLabel label && text.equals(label.getText())) return label;
      if (child instanceof Container nested) {
        JLabel match = findLabel(nested, text);
        if (match != null) return match;
      }
    }
    return null;
  }
  private static JList<?> findList(Container parent) {
    for (Component child : parent.getComponents()) {
      if (child instanceof JList<?> list) return list;
      if (child instanceof Container nested) {
        JList<?> match = findList(nested);
        if (match != null) return match;
      }
    }
    return null;
  }

  private static JTable findTable(Container parent, String first, String second, String third) {
    for (Component child : parent.getComponents()) {
      if (child instanceof JTable table && table.getColumnCount() >= 3
          && first.equals(table.getColumnName(0)) && second.equals(table.getColumnName(1))
          && third.equals(table.getColumnName(2))) return table;
      if (child instanceof Container nested) {
        JTable match = findTable(nested, first, second, third);
        if (match != null) return match;
      }
    }
    return null;
  }

  private static JTextArea findEditableTextArea(Container parent) {
    for (Component child : parent.getComponents()) {
      if (child instanceof JTextArea area && area.isEditable()) return area;
      if (child instanceof Container nested) {
        JTextArea match = findEditableTextArea(nested);
        if (match != null) return match;
      }
    }
    return null;
  }

  private static JTextArea findTextArea(Container parent) {
    for (Component child : parent.getComponents()) {
      if (child instanceof JTextArea area) return area;
      if (child instanceof Container nested) {
        JTextArea match = findTextArea(nested);
        if (match != null) return match;
      }
    }
    return null;
  }

  private static JTabbedPane findTabs(Container parent) {
    for (Component child : parent.getComponents()) {
      if (child instanceof JTabbedPane tabs) return tabs;
      if (child instanceof Container nested) {
        JTabbedPane match = findTabs(nested);
        if (match != null) return match;
      }
    }
    return null;
  }

  private static JToggleButton findToggle(Container parent, String name) {
    for (Component child : parent.getComponents()) {
      if (child instanceof JToggleButton toggle && toggle.getText().contains(name)) return toggle;
      if (child instanceof Container nested) {
        JToggleButton match = findToggle(nested, name);
        if (match != null) return match;
      }
    }
    return null;
  }
  private static JButton findButton(Container parent, String text) {
    for (Component child : parent.getComponents()) {
      if (child instanceof JButton button && text.equals(button.getText())) return button;
      if (child instanceof Container nested) {
        JButton match = findButton(nested, text);
        if (match != null) return match;
      }
    }
    return null;
  }

  private static void save(Window window, Path file) {
    try {
      BufferedImage image = new BufferedImage(window.getWidth(), window.getHeight(), BufferedImage.TYPE_INT_RGB);
      var graphics = image.createGraphics();
      window.printAll(graphics);
      graphics.dispose();
      ImageIO.write(image, "png", file.toFile());
    } catch (java.io.IOException error) { throw new RuntimeException(error); }
  }
}

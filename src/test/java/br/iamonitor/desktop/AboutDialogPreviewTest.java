package br.iamonitor.desktop;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AboutDialogPreviewTest {
  @Test void mostraArquiteturaDecisoesEOrigemDaContagem(@TempDir Path temp) throws Exception {
    Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    Path project = Files.createDirectory(temp.resolve("project"));
    Path logs = Files.createDirectory(temp.resolve("logs"));
    String cwd = project.toString().replace("\\", "\\\\");
    Files.writeString(logs.resolve("development.jsonl"),
        "{\"type\":\"session_meta\",\"payload\":{\"cwd\":\"" + cwd + "\"}}\n"
            + "{\"type\":\"event_msg\",\"payload\":{\"type\":\"token_count\",\"info\":{\"total_token_usage\":{"
            + "\"input_tokens\":120000000,\"cached_input_tokens\":20000000,\"output_tokens\":30000000,\"total_tokens\":150000000}}}}\n");
    JFrame frame = new JFrame("IAMonitor");
    SwingUtilities.invokeAndWait(() -> {
      frame.setSize(1200, 820);
      frame.setLocationRelativeTo(null);
      frame.setVisible(true);
    });
    JDialog dialog = null;
    try {
      SwingUtilities.invokeLater(() -> AboutDialog.open(frame, null, project, logs));
      for (int i = 0; i < 60 && dialog == null; i++) {
        Thread.sleep(100);
        for (Window window : Window.getWindows())
          if (window instanceof JDialog candidate && candidate.isShowing()
              && "Sobre o IAMonitor".equals(candidate.getTitle())) dialog = candidate;
      }
      assertNotNull(dialog);
      JDialog shown = dialog;
      SwingUtilities.invokeAndWait(() -> {
        assertNotNull(findLabel(shown, "Como o IAMonitor foi construído"));
        assertNotNull(findButton(shown, "Arquitetura"));
        save(shown, Path.of("target/about-overview-preview.png"));
        findButton(shown, "Arquitetura").doClick();
        assertNotNull(findLabel(shown, "O caminho do pedido"));
        save(shown, Path.of("target/about-architecture-preview.png"));
        findButton(shown, "Decisões").doClick();
        assertNotNull(findLabel(shown, "Decisões centrais"));
        findButton(shown, "IA e tokens").doClick();
        assertNotNull(findLabel(shown, "Construção com assistência de IA"));
      });
      for (int i = 0; i < 40 && findLabel(shown, "150.000.000") == null; i++) Thread.sleep(100);
      SwingUtilities.invokeAndWait(() -> {
        assertNotNull(findLabel(shown, "150.000.000"));
        assertNotNull(findLabel(shown, "120.000.000"));
        assertNotNull(findLabel(shown, "30.000.000"));
        assertTrue(findLabel(shown, "CACHE NA ENTRADA").getParent().getMouseListeners().length > 0);
        save(shown, Path.of("target/about-tokens-preview.png"));
      });
      assertTrue(Files.size(Path.of("target/about-architecture-preview.png")) > 10000);
      assertTrue(Files.size(Path.of("target/about-tokens-preview.png")) > 10000);
    } finally {
      JDialog opened = dialog;
      SwingUtilities.invokeAndWait(() -> {
        if (opened != null) opened.dispose();
        frame.dispose();
      });
    }
  }

  private static JLabel findLabel(Container parent, String text) {
    for (Component child : parent.getComponents()) {
      if (child instanceof JLabel label && label.getText().contains(text)) return label;
      if (child instanceof Container nested) {
        JLabel match = findLabel(nested, text);
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

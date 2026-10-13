package br.iamonitor.desktop;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DevelopmentCacheInspectorPreviewTest {
  @Test void renderizaRegistrosELinhasCumulativas(@TempDir Path temp) throws Exception {
    Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
    Path logs = temp.resolve("logs");
    DevelopmentUsage.Entry entry = new DevelopmentUsage.Entry(logs.resolve("one.jsonl"), List.of(
        new DevelopmentUsage.Sample(8, 120, 30, 20, 140),
        new DevelopmentUsage.Sample(16, 250, 80, 40, 290)));
    DevelopmentUsage.Breakdown breakdown = new DevelopmentUsage.Breakdown(
        new DevelopmentUsage.Result(1, 250, 80, 40, 290), List.of(entry));
    SwingUtilities.invokeAndWait(() -> {
      JFrame frame = new JFrame("Prévia do cache de desenvolvimento");
      JPanel content = DevelopmentCacheInspector.content(frame, breakdown, logs);
      frame.setContentPane(content);
      frame.setSize(940, 640);
      frame.setVisible(true);
      frame.validate();
      try {
        BufferedImage image = new BufferedImage(content.getWidth(), content.getHeight(),
            BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        content.printAll(graphics);
        graphics.dispose();
        ImageIO.write(image, "png", Path.of("target/development-cache-preview.png").toFile());
      } catch (Exception error) { throw new RuntimeException(error); }
      frame.dispose();
    });
    assertTrue(Files.size(Path.of("target/development-cache-preview.png")) > 10000);
  }
}

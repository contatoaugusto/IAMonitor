package br.iamonitor.desktop;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CacheInspectorPreviewTest {
  @Test void renderizaInspecaoSemInventarConteudo(@TempDir Path temp) throws Exception {
    Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
    Path project = Files.createDirectory(temp.resolve("project"));
    try (Store store = new Store(temp.resolve("data"), new EventLogic())) {
      Store.Run run = store.begin(project, "Pedido fictício", "", "read-only");
      ObjectMapper json = new ObjectMapper();
      for (String raw : new String[]{
          "{\"type\":\"turn.started\"}",
          "{\"type\":\"item.completed\",\"item\":{\"type\":\"agent_message\",\"text\":\"Evento observado de teste\"}}",
          "{\"type\":\"turn.completed\",\"usage\":{\"input_tokens\":1200,\"cached_input_tokens\":300,\"output_tokens\":450,\"total_tokens\":1650}}",
          "{\"type\":\"turn.started\"}",
          "{\"type\":\"turn.completed\",\"usage\":{\"input_tokens\":800,\"cached_input_tokens\":600,\"output_tokens\":100,\"total_tokens\":900}}"})
        store.recordEvent(run.id(), json.readTree(raw), raw);
      Store.Detail detail = store.reportDetail(run.id());
      Map<Integer, String> payloads = new HashMap<>();
      for (Store.Event event : detail.events())
        if ("turn.completed".equals(event.type()))
          payloads.put(event.seq(), store.eventPayload(run.id(), event.seq()));
      CacheEvidence.Snapshot evidence = CacheEvidence.from(detail, payloads, true);
      assertTrue(CacheInspector.describe(evidence.units().get(0), evidence)
          .contains("Sequência exata dos tokens em cache: indisponível"));
      SwingUtilities.invokeAndWait(() -> {
        JFrame frame = new JFrame("Prévia da inspeção de cache");
        JPanel content = CacheInspector.content(frame, store, evidence, 1);
        frame.setContentPane(content);
        frame.setSize(1050, 730);
        frame.setVisible(true);
        try {
          BufferedImage preview = new BufferedImage(content.getWidth(), content.getHeight(),
              BufferedImage.TYPE_INT_RGB);
          var graphics = preview.createGraphics();
          content.printAll(graphics);
          graphics.dispose();
          ImageIO.write(preview, "png", Path.of("target/cache-inspector-preview.png").toFile());
        } catch (Exception error) { throw new RuntimeException(error); }
        frame.dispose();
      });
      assertTrue(Files.size(Path.of("target/cache-inspector-preview.png")) > 10000);
    }
  }
}

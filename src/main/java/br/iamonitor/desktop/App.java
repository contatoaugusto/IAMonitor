package br.iamonitor.desktop;

import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class App {
  private App() {}

  public static void main(String[] args) {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      Path data = dataDir(args);
      EventLogic logic = new EventLogic();
      Store store = new Store(data, logic);
      SwingUtilities.invokeLater(() -> new Dashboard(store).setVisible(true));
    } catch (Exception error) {
      JOptionPane.showMessageDialog(null, error.getMessage(), "IAMonitor não iniciou", JOptionPane.ERROR_MESSAGE);
      System.exit(1);
    }
  }

  static Path dataDir(String[] args) {
    if (args.length == 2 && "--data-dir".equals(args[0])) return Path.of(args[1]).toAbsolutePath().normalize();
    String configured = System.getenv("IAMONITOR_DATA_DIR");
    if (configured != null && !configured.isBlank()) return Path.of(configured).toAbsolutePath().normalize();
    Path cwd = Path.of("").toAbsolutePath().normalize();
    Path jar = null;
    try { jar = Path.of(App.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toAbsolutePath(); }
    catch (Exception ignored) { /* Running from a development classpath. */ }
    for (Path start : new Path[]{cwd, jar == null ? null : jar.getParent()}) {
      for (Path cursor = start; cursor != null; cursor = cursor.getParent()) {
        Path candidate = cursor.resolve("data");
        if (Files.isRegularFile(candidate.resolve("iamonitor.sqlite"))) return candidate;
        if (Files.isRegularFile(cursor.resolve("AGENTS.md")) && Files.isDirectory(cursor.resolve("desktop"))) return candidate;
      }
    }
    String local = System.getenv("LOCALAPPDATA");
    return local == null ? cwd.resolve("data") : Path.of(local, "IAMonitor", "data");
  }
}

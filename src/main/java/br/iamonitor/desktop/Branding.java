package br.iamonitor.desktop;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import javax.imageio.ImageIO;

record Branding(String name, String subtitle, BufferedImage logo) {
  static Branding load() {
    Properties settings = new Properties();
    try (InputStream input = Branding.class.getResourceAsStream("/branding.properties")) {
      if (input != null) settings.load(new InputStreamReader(input, StandardCharsets.UTF_8));
    } catch (Exception ignored) { /* The bundled defaults remain available. */ }
    String name = setting("IAMONITOR_CHANNEL_NAME", settings, "channel.name", "IA Monitor");
    String subtitle = setting("IAMONITOR_CHANNEL_SUBTITLE", settings, "channel.subtitle",
        "Acompanhamento de sessões do Codex");
    BufferedImage logo = image("IAMONITOR_CHANNEL_LOGO", settings.getProperty("channel.logo", ""));
    return new Branding(name, subtitle, logo == null ? lettermark(name) : logo);
  }

  private static BufferedImage lettermark(String name) {
    BufferedImage image = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
    Graphics2D graphics = image.createGraphics();
    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    graphics.setColor(new Color(16, 36, 49));
    graphics.fillRoundRect(0, 0, 255, 255, 54, 54);
    graphics.setColor(new Color(128, 184, 220));
    graphics.drawRoundRect(7, 7, 241, 241, 48, 48);
    String letters = "Dev Terceira Idade".equalsIgnoreCase(name) ? "D3" : name.substring(0, 1).toUpperCase();
    graphics.setFont(new Font("Segoe UI", Font.BOLD, 126));
    FontMetrics metrics = graphics.getFontMetrics();
    graphics.setColor(new Color(244, 242, 236));
    graphics.drawString(letters, (256 - metrics.stringWidth(letters)) / 2,
        (256 - metrics.getHeight()) / 2 + metrics.getAscent());
    graphics.dispose();
    return image;
  }

  private static String setting(String environment, Properties properties, String key, String fallback) {
    String override = System.getenv(environment);
    if (override != null && !override.isBlank()) return override.strip();
    String configured = properties.getProperty(key);
    return configured == null || configured.isBlank() ? fallback : configured.strip();
  }

  private static BufferedImage image(String environment, String resource) {
    String external = System.getenv(environment);
    if (external != null && !external.isBlank()) {
      try {
        Path path = Path.of(external);
        if (Files.isRegularFile(path)) return ImageIO.read(path.toFile());
      } catch (Exception ignored) { /* Continue with bundled branding. */ }
    }
    if (resource.isBlank()) return null;
    try (InputStream input = Branding.class.getResourceAsStream(resource.startsWith("/") ? resource : "/" + resource)) {
      return input == null ? null : ImageIO.read(input);
    } catch (Exception ignored) {
      return null;
    }
  }
}

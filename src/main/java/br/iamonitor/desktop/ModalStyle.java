package br.iamonitor.desktop;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Window;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.RootPaneContainer;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

final class ModalStyle {
  static final Color BACKGROUND = new Color(243, 238, 227);
  static final Color PAPER = new Color(255, 253, 248);
  static final Color INK = new Color(28, 48, 56);
  static final Color MUTED = new Color(79, 93, 99);
  static final Color HEADER = new Color(123, 66, 46);
  static final Color ACCENT = new Color(169, 84, 47);
  static final Color BORDER = new Color(191, 143, 91);

  private ModalStyle() {}

  static JPanel panel() {
    JPanel panel = new JPanel();
    panel.setBackground(BACKGROUND);
    return panel;
  }

  static JButton button(String text, boolean primary) {
    JButton button = new JButton(text);
    button.setUI(new javax.swing.plaf.basic.BasicButtonUI());
    button.setFont(new Font("Segoe UI", Font.BOLD, 12));
    button.setBackground(primary ? ACCENT : PAPER);
    button.setForeground(primary ? Color.WHITE : INK);
    button.setFocusPainted(false);
    button.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(primary ? ACCENT : BORDER, 1),
        new EmptyBorder(9, 17, 9, 17)));
    return button;
  }

  static void show(Window owner, JDialog dialog) {
    RootPaneContainer root = (RootPaneContainer) owner;
    java.awt.Component previous = root.getGlassPane();
    boolean wasVisible = previous.isVisible();
    JPanel shade = new JPanel() {
      @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        graphics.setColor(new Color(0, 0, 0, 132));
        graphics.fillRect(0, 0, getWidth(), getHeight());
      }
    };
    shade.setOpaque(false);
    root.setGlassPane(shade);
    shade.setVisible(true);
    try {
      dialog.setVisible(true);
    } finally {
      root.setGlassPane(previous);
      previous.setVisible(wasVisible);
    }
  }
}
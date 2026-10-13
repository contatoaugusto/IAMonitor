package br.iamonitor.desktop;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

final class DetailsDialog {
  private DetailsDialog() {}

  static void open(Window owner, String title, String headingText, String subtitle,
      Supplier<String> description) {
    JDialog dialog = new JDialog(owner, title, java.awt.Dialog.ModalityType.APPLICATION_MODAL);
    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    JPanel body = ModalStyle.panel();
    body.setLayout(new BorderLayout(0, 15));
    body.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(ModalStyle.BORDER, 3),
        new EmptyBorder(20, 24, 18, 24)));
    JPanel heading = ModalStyle.panel();
    heading.setBackground(ModalStyle.HEADER);
    heading.setBorder(new EmptyBorder(13, 16, 14, 16));
    heading.setLayout(new GridLayout(2, 1, 0, 4));
    heading.add(label("DETALHES  ·  " + headingText, 21, Font.BOLD, Color.WHITE));
    heading.add(label(subtitle, 11, Font.PLAIN, new Color(255, 221, 180)));
    body.add(heading, BorderLayout.NORTH);
    JTextArea details = new JTextArea("Carregando detalhes...");
    details.setEditable(false);
    details.setLineWrap(true);
    details.setWrapStyleWord(true);
    details.setFont(new Font("Consolas", Font.PLAIN, 14));
    details.setBackground(ModalStyle.PAPER);
    details.setForeground(ModalStyle.INK);
    details.setCaretColor(ModalStyle.ACCENT);
    details.setBorder(new EmptyBorder(15, 17, 15, 17));
    JScrollPane scroll = new JScrollPane(details);
    scroll.setBorder(BorderFactory.createLineBorder(ModalStyle.BORDER));
    body.add(scroll, BorderLayout.CENTER);
    JPanel footer = ModalStyle.panel();
    footer.setLayout(new FlowLayout(FlowLayout.RIGHT, 0, 0));
    JButton close = ModalStyle.button("Fechar", true);
    close.addActionListener(event -> dialog.dispose());
    footer.add(close);
    body.add(footer, BorderLayout.SOUTH);
    dialog.setContentPane(body);
    dialog.setSize(820, 640);
    dialog.setMinimumSize(new Dimension(640, 450));
    dialog.setLocationRelativeTo(owner);
    new SwingWorker<String, Void>() {
      @Override protected String doInBackground() { return description.get(); }
      @Override protected void done() {
        if (!dialog.isDisplayable()) return;
        try {
          details.setText(get());
          details.setCaretPosition(0);
        } catch (Exception error) {
          details.setText("Não foi possível carregar os detalhes: " + error.getMessage());
        }
      }
    }.execute();
    ModalStyle.show(owner, dialog);
  }

  private static JLabel label(String text, int size, int style, Color color) {
    JLabel label = new JLabel(text);
    label.setFont(new Font("Segoe UI", style, size));
    label.setForeground(color);
    return label;
  }
}

package br.iamonitor.desktop;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.net.URI;
import java.text.NumberFormat;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

final class TokenMethodologyPanel {
  private static final Color BG = ModalStyle.BACKGROUND;
  private static final Color PAPER = ModalStyle.PAPER;
  private static final Color INK = ModalStyle.INK;
  private static final Color MUTED = ModalStyle.MUTED;
  private static final Color ACCENT = ModalStyle.ACCENT;

  private TokenMethodologyPanel() {}

  static JScrollPane create(String provider) {
    JPanel content = new JPanel();
    content.setBackground(BG);
    content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
    content.setBorder(new EmptyBorder(23, 27, 28, 27));

    content.add(heading("Como cada ferramenta conta os tokens", 20));
    content.add(gap(5));
    content.add(note("A tokenização depende do modelo e do conteúdo enviado. Escolha uma ferramenta para ver o método e depois compare apenas o texto digitado.", 3));
    content.add(gap(18));

    JPanel selector = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
    selector.setBackground(BG);
    selector.setAlignmentX(JPanel.LEFT_ALIGNMENT);
    selector.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
    selector.add(label("FERRAMENTA", 12, Font.BOLD, INK));
    JComboBox<Choice> choices = new JComboBox<>();
    for (String id : Provider.IDS) choices.addItem(new Choice(id, Provider.name(id)));
    choices.setFont(new Font("Segoe UI", Font.PLAIN, 14));
    choices.setBackground(PAPER);
    choices.setForeground(INK);
    choices.setPreferredSize(new Dimension(265, 34));
    for (int index = 0; index < choices.getItemCount(); index++)
      if (choices.getItemAt(index).id().equals(provider)) choices.setSelectedIndex(index);
    selector.add(choices);
    content.add(selector);
    content.add(gap(12));

    JPanel explanation = new JPanel();
    explanation.setOpaque(false);
    explanation.setLayout(new BoxLayout(explanation, BoxLayout.Y_AXIS));
    explanation.setAlignmentX(JPanel.LEFT_ALIGNMENT);
    content.add(explanation);

    Runnable update = () -> {
      Choice selected = (Choice) choices.getSelectedItem();
      if (selected == null) return;
      TokenMethodology.Explanation method = TokenMethodology.explanation(selected.id());
      explanation.removeAll();
      explanation.add(heading(method.title(), 17));
      explanation.add(gap(9));
      explanation.add(card("COMO O TEXTO É DIVIDIDO", method.how(), 4));
      explanation.add(gap(9));
      explanation.add(card("COMO OBTER A CONTAGEM OFICIAL", method.official(), 3));
      explanation.add(gap(9));
      explanation.add(card("O QUE ESTE SIMULADOR NÃO MEDE", method.limits(), 3));
      explanation.add(gap(9));
      JButton source = ModalStyle.button("Abrir fonte oficial", false);
      source.setToolTipText(method.url());
      source.addActionListener(event -> openSource(method.url()));
      JPanel sourceRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
      sourceRow.setOpaque(false);
      sourceRow.setAlignmentX(JPanel.LEFT_ALIGNMENT);
      sourceRow.add(source);
      sourceRow.add(Box.createHorizontalStrut(10));
      sourceRow.add(label(method.source(), 11, Font.PLAIN, MUTED));
      explanation.add(sourceRow);
      explanation.revalidate();
      explanation.repaint();
    };
    choices.addActionListener(event -> update.run());
    update.run();
    content.add(gap(27));

    content.add(heading("Simule o texto do pedido", 17));
    content.add(gap(8));
    content.add(note("Cole somente o texto que quer comparar. O cálculo acontece neste computador; nada é enviado às empresas de IA.", 2));
    content.add(gap(10));
    JTextArea input = new JTextArea(5, 65);
    input.setFont(new Font("Segoe UI", Font.PLAIN, 14));
    input.setLineWrap(true);
    input.setWrapStyleWord(true);
    input.setBackground(PAPER);
    input.setForeground(INK);
    input.setCaretColor(INK);
    input.setMargin(new java.awt.Insets(10, 12, 10, 12));
    input.setToolTipText("Digite ou cole um texto de até 20.000 caracteres");
    JScrollPane inputScroll = new JScrollPane(input);
    inputScroll.setAlignmentX(JPanel.LEFT_ALIGNMENT);
    inputScroll.setBorder(BorderFactory.createLineBorder(ModalStyle.BORDER));
    inputScroll.setPreferredSize(new Dimension(900, 115));
    inputScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 115));
    content.add(inputScroll);
    content.add(gap(11));

    JButton calculate = ModalStyle.button("Calcular tokens", true);
    JLabel status = label("Aguardando um texto para comparar.", 12, Font.PLAIN, MUTED);
    JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
    controls.setOpaque(false);
    controls.setAlignmentX(JPanel.LEFT_ALIGNMENT);
    controls.setMaximumSize(new Dimension(Integer.MAX_VALUE, 39));
    controls.add(calculate);
    controls.add(status);
    content.add(controls);
    content.add(gap(15));

    JPanel results = new JPanel();
    results.setOpaque(false);
    results.setLayout(new BoxLayout(results, BoxLayout.Y_AXIS));
    results.setAlignmentX(JPanel.LEFT_ALIGNMENT);
    content.add(results);
    calculate.addActionListener(event -> {
      String text = input.getText();
      results.removeAll();
      if (text.isBlank()) {
        status.setText("Digite um texto antes de calcular.");
      } else if (text.length() > 20_000) {
        status.setText("Use até 20.000 caracteres por comparação.");
      } else {
        status.setText("Comparação de texto isolado · não altera esta sessão.");
        for (TokenMethodology.Result result : TokenMethodology.compare(text)) {
          results.add(resultCard(result));
          results.add(gap(8));
        }
      }
      results.revalidate();
      results.repaint();
    });
    content.add(gap(9));
    content.add(note("≈ indica aproximação, não uso medido. OpenAI: contagem local para o200k_base; "
        + "Claude, Cursor e Copilot: a mesma codificação como referência comparativa; "
        + "Gemini: cerca de quatro caracteres por token. Modelos, instruções e arquivos podem alterar os totais reais.", 3));

    JScrollPane scroll = new JScrollPane(content);
    scroll.setBorder(null);
    scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    scroll.getVerticalScrollBar().setUnitIncrement(18);
    return scroll;
  }

  private static JPanel card(String title, String body, int rows) {
    JPanel panel = new JPanel(new BorderLayout(0, 6));
    panel.setBackground(PAPER);
    panel.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(new Color(213, 198, 177)),
        new EmptyBorder(12, 15, 12, 15)));
    panel.setAlignmentX(JPanel.LEFT_ALIGNMENT);
    panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, rows * 24 + 49));
    panel.add(label(title, 11, Font.BOLD, ACCENT), BorderLayout.NORTH);
    panel.add(note(body, rows), BorderLayout.CENTER);
    return panel;
  }

  private static JPanel resultCard(TokenMethodology.Result result) {
    JPanel panel = new JPanel(new BorderLayout(16, 0));
    panel.setBackground(PAPER);
    panel.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(new Color(213, 198, 177)),
        new EmptyBorder(10, 15, 10, 15)));
    panel.setAlignmentX(JPanel.LEFT_ALIGNMENT);
    panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 68));
    JPanel text = new JPanel(new BorderLayout(0, 3));
    text.setOpaque(false);
    text.add(label(result.name(), 13, Font.BOLD, INK), BorderLayout.NORTH);
    text.add(label(result.method(), 11, Font.PLAIN, MUTED), BorderLayout.SOUTH);
    panel.add(text, BorderLayout.CENTER);
    String amount = (result.encodingCount() ? "" : "≈ ")
        + NumberFormat.getIntegerInstance(Locale.forLanguageTag("pt-BR")).format(result.tokens());
    JLabel number = label(amount, 24, Font.BOLD, result.encodingCount() ? ACCENT : INK);
    number.setHorizontalAlignment(SwingConstants.RIGHT);
    panel.add(number, BorderLayout.EAST);
    return panel;
  }

  private static JTextArea note(String text, int rows) {
    JTextArea area = new JTextArea(text, rows, 0);
    area.setEditable(false);
    area.setLineWrap(true);
    area.setWrapStyleWord(true);
    area.setOpaque(false);
    area.setFont(new Font("Segoe UI", Font.PLAIN, 13));
    area.setForeground(MUTED);
    area.setAlignmentX(JPanel.LEFT_ALIGNMENT);
    return area;
  }

  private static JLabel heading(String text, int size) {
    JLabel label = label(text, size, Font.BOLD, INK);
    label.setAlignmentX(JPanel.LEFT_ALIGNMENT);
    return label;
  }

  private static JLabel label(String text, int size, int style, Color color) {
    JLabel label = new JLabel(text);
    label.setFont(new Font("Segoe UI", style, size));
    label.setForeground(color);
    return label;
  }

  private static java.awt.Component gap(int height) {
    return Box.createVerticalStrut(height);
  }

  private static void openSource(String url) {
    try {
      if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
        Desktop.getDesktop().browse(URI.create(url));
    } catch (Exception ignored) {
      // The URL remains available as the button tooltip when no browser can be opened.
    }
  }

  private record Choice(String id, String name) {
    @Override public String toString() { return name; }
  }
}

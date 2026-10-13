package br.iamonitor.desktop;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.function.IntConsumer;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

final class SessionFlowPanel {
  private static final Color INPUT = new Color(31, 91, 126);
  private static final Color MODEL = new Color(81, 65, 117);
  private static final Color OUTPUT = new Color(145, 77, 37);
  private static final Color LOCAL = new Color(55, 94, 75);
  private static final NumberFormat NUM =
      NumberFormat.getIntegerInstance(Locale.forLanguageTag("pt-BR"));

  private SessionFlowPanel() {}

  static JPanel create(SessionFlow.Snapshot flow) {
    return create(flow, ignored -> {});
  }

  static JPanel create(SessionFlow.Snapshot flow, IntConsumer onCacheSelected) {
    JPanel content = new JPanel();
    content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
    content.setBackground(ModalStyle.BACKGROUND);
    content.setAlignmentX(JPanel.LEFT_ALIGNMENT);
    int height = 105 + 11 + (flow.cycles().isEmpty() ? 68 : flow.cycles().size() * 141) + 68;
    content.setPreferredSize(new Dimension(900, height));
    content.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));

    JPanel summary = new JPanel(new BorderLayout(0, 8));
    summary.setBackground(ModalStyle.PAPER);
    summary.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(ModalStyle.BORDER),
        new EmptyBorder(14, 17, 13, 17)));
    summary.setAlignmentX(JPanel.LEFT_ALIGNMENT);
    summary.setPreferredSize(new Dimension(900, 105));
    summary.setMaximumSize(new Dimension(Integer.MAX_VALUE, 105));
    String unit = flow.unit();
    String title = flow.completed() + " " + (flow.completed() == 1
        ? unit + " concluído" : ("turno".equals(unit) ? "turnos" : "resultados de uso") + " concluídos")
        + (flow.pending() > 0 ? "  ·  " + flow.pending() + " em andamento" : "")
        + (flow.messageEvents() > 0 ? "  ·  " + flow.messageEvents() + " eventos de mensagem" : "");
    summary.add(label(title, 16, Font.BOLD, ModalStyle.INK), BorderLayout.NORTH);
    summary.add(note(flow.fullHistory()
        ? "Cada faixa abaixo é um turno ou resultado de uso informado pela CLI. Ela pode reunir várias chamadas internas; a quantidade de requisições de rede e de fragmentos de resposta não é observável."
        : "Não foi possível consultar o histórico completo. Nenhuma quantidade de chamadas remotas é presumida.",
        3), BorderLayout.CENTER);
    content.add(summary);
    content.add(gap(11));

    if (flow.cycles().isEmpty()) {
      content.add(info("Ainda não há turnos ou resultados de uso registrados para esta sessão."));
    } else {
      int index = 0;
      for (SessionFlow.Cycle cycle : flow.cycles()) {
        content.add(cycle(++index, cycle, onCacheSelected));
        content.add(gap(9));
      }
    }
    content.add(info("Entrada é contabilizada em cada unidade reportada, inclusive quando há contexto repetido; "
        + "os eventos não mostram quais trechos foram reenviados. Saída é o que o modelo gerou, não o número de "
        + "mensagens ou pacotes recebidos. Eventos de mensagem são registros da CLI, não respostas de rede. Cache já integra a entrada."));
    return content;
  }

  private static JPanel cycle(int index, SessionFlow.Cycle cycle, IntConsumer onCacheSelected) {
    EventLogic.Usage usage = cycle.usage();
    JPanel panel = new JPanel(new BorderLayout(0, 9));
    panel.setBackground(ModalStyle.PAPER);
    panel.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(new Color(194, 175, 151)),
        new EmptyBorder(11, 14, 11, 14)));
    panel.setAlignmentX(JPanel.LEFT_ALIGNMENT);
    panel.setPreferredSize(new Dimension(900, 132));
    panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 132));

    String status = !cycle.completed() ? "AGUARDANDO CONCLUSÃO"
        : usage == null ? "CONCLUÍDO SEM CONTAGEM" : "USO REPORTADO";
    JLabel title = label(String.format(Locale.ROOT, "%02d", index) + "  ·  "
        + time(cycle.at()) + "  ·  " + status, 11, Font.BOLD, ModalStyle.MUTED);
    panel.add(title, BorderLayout.NORTH);

    JPanel stages = new JPanel(new GridLayout(1, 4, 7, 0));
    stages.setOpaque(false);
    stages.add(step("CLI → MODELO", usage == null ? "—" : num(usage.input()) + " entrada", INPUT));
    stages.add(step("MODELO REMOTO", cycle.completed() ? "processou" : "aguardando", MODEL));
    stages.add(step("MODELO → CLI", usage == null ? "—" : num(usage.output()) + " saída", OUTPUT));
    stages.add(step("RESULTADO", usage == null ? "—" : num(usage.total()) + " tokens", LOCAL));
    panel.add(stages, BorderLayout.CENTER);

    String footer = usage == null ? "Tokens indisponíveis; zero não é presumido."
        : num(usage.cached()) + " tokens de cache já incluídos na entrada"
            + (usage.total() > usage.input() + usage.output()
                ? "  ·  " + num(usage.total() - usage.input() - usage.output())
                    + " sem detalhamento" : "");
    if (cycle.messageEvents() > 0)
      footer += "  ·  " + cycle.messageEvents() + " evento(s) de mensagem no intervalo";
    JLabel cacheLabel = label(footer, usage == null ? 11 : 12,
        usage == null ? Font.PLAIN : Font.BOLD, usage == null ? ModalStyle.MUTED : INPUT);
    if (usage != null) {
      cacheLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      cacheLabel.setToolTipText("Clique para inspecionar o cache deste resultado.");
      cacheLabel.addMouseListener(new MouseAdapter() {
        @Override public void mouseClicked(MouseEvent event) { onCacheSelected.accept(cycle.seq()); }
      });
    }
    panel.add(cacheLabel, BorderLayout.SOUTH);
    return panel;
  }

  private static JPanel step(String title, String value, Color color) {
    JPanel panel = new JPanel(new GridLayout(2, 1, 0, 3));
    panel.setBackground(color);
    panel.setBorder(new EmptyBorder(6, 10, 7, 10));
    panel.add(label(title, 10, Font.BOLD, Color.WHITE));
    panel.add(label(value, 16, Font.BOLD, Color.WHITE));
    return panel;
  }

  private static JPanel info(String text) {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBackground(ModalStyle.BACKGROUND);
    panel.setAlignmentX(JPanel.LEFT_ALIGNMENT);
    panel.setPreferredSize(new Dimension(900, 68));
    panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 68));
    panel.add(note(text, 3), BorderLayout.CENTER);
    return panel;
  }

  private static JTextArea note(String text, int rows) {
    JTextArea area = new JTextArea(text, rows, 0);
    area.setEditable(false);
    area.setLineWrap(true);
    area.setWrapStyleWord(true);
    area.setOpaque(false);
    area.setFont(new Font("Segoe UI", Font.PLAIN, 12));
    area.setForeground(ModalStyle.MUTED);
    return area;
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

  private static String num(long value) { return NUM.format(value); }

  private static String time(String at) {
    return at != null && at.length() >= 19 ? at.substring(11, 19) : "horário indisponível";
  }
}

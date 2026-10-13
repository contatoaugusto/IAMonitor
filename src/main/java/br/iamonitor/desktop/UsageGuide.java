package br.iamonitor.desktop;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

final class UsageGuide {
  private static final Color BG = ModalStyle.BACKGROUND;
  private static final Color PANEL = new Color(25, 54, 68);
  private static final Color INK = new Color(244, 242, 236);
  private static final Color MUTED = new Color(183, 201, 209);
  private static final Color INPUT = new Color(128, 184, 220);
  private static final Color OUTPUT = new Color(238, 177, 111);
  private static final NumberFormat NUM = NumberFormat.getIntegerInstance(Locale.forLanguageTag("pt-BR"));

  private UsageGuide() {}

  static void open(JFrame owner, Store store, Store.Detail detail) {
    JDialog dialog = new JDialog(owner, "Como são medidos os tokens", true);
    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    JPanel root = new JPanel(new BorderLayout(0, 0));
    root.setBackground(BG);
    root.setBorder(BorderFactory.createLineBorder(ModalStyle.BORDER, 3));
    JPanel header = new JPanel(new GridLayout(3, 1, 0, 3));
    header.setBackground(ModalStyle.HEADER);
    header.setBorder(new EmptyBorder(15, 27, 17, 27));
    header.add(label("GUIA VISUAL  ·  JANELA DE AJUDA", 11, Font.BOLD, new Color(255, 217, 163)));
    header.add(label("Como são medidos os tokens", 23, Font.BOLD, Color.WHITE));
    header.add(label("Explore o fluxo da sessão e compare a tokenização das cinco ferramentas de IA.", 13, Font.PLAIN, Color.WHITE));
    root.add(header, BorderLayout.NORTH);

    GuideContent content = new GuideContent();
    content.setBackground(BG);
    content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
    content.setBorder(new EmptyBorder(19, 27, 24, 27));
    Store.Run run = detail == null ? null : detail.run();
    java.util.List<Store.Event> flowEvents = java.util.List.of();
    boolean fullFlowHistory = true;
    if (run != null) {
      try { flowEvents = store.flowEvents(run.id()); }
      catch (java.sql.SQLException error) {
        flowEvents = detail.events();
        fullFlowHistory = false;
      }
    }
    SessionFlow.Snapshot observedFlow = SessionFlow.from(
        flowEvents, run == null ? "codex" : run.provider(), fullFlowHistory);
    boolean reported = run != null && (run.totalTokens() > 0 || run.inputTokens() + run.outputTokens() > 0
        || detail.events().stream().anyMatch(event -> event.usage() != null));
    TokenAccounting.Assessment count = run == null ? null
        : TokenAccounting.assess(run, reported, detail.usageAuditable());
    boolean hasAmounts = count != null && !count.total().equals("—") && detail.usageAuditable();
    long input = hasAmounts ? run.inputTokens() : -1;
    long output = hasAmounts ? run.outputTokens() : -1;
    JourneyCounts journeyCounts = JourneyCounts.from(run, count, hasAmounts);
    String provider = run == null ? "a IA escolhida" : Provider.name(run.provider());
    Consumer<Integer> openCache = sequence -> CacheInspector.open(dialog, store, detail, sequence);
    content.add(heading("Quantas vezes o caminho foi registrado"));
    content.add(Box.createVerticalStrut(10));
    content.add(SessionFlowPanel.create(observedFlow, sequence -> openCache.accept(sequence)));
    content.add(Box.createVerticalStrut(22));
    content.add(heading("Mapa do caminho entre a CLI e o modelo"));
    content.add(Box.createVerticalStrut(10));
    IntConsumer openStage = stage -> {
      if (stage == 9) {
        openCache.accept(null);
        return;
      }
      DetailsDialog.open(dialog,
          "Detalhes da etapa: " + StageDetails.title(stage), StageDetails.title(stage),
          detail == null ? "Selecione uma sessão" :
              "Sessão " + detail.run().id().substring(0, 8).toUpperCase(),
          () -> StageDetails.describe(store, detail, stage));
    };
    JourneyDiagram journey = new JourneyDiagram(provider, journeyCounts, openStage);
    journey.setAlignmentX(JPanel.LEFT_ALIGNMENT);
    content.add(journey);
    content.add(Box.createVerticalStrut(12));
    content.add(paragraph("Os números com * são totais da sessão informados pela CLI e aparecem em mais de uma etapa para explicar o fluxo: não devem ser somados novamente. Pedido, arquivos e ações locais não têm contagem real separada; rede e download não são medidos em tokens.", 12, ModalStyle.MUTED, 3));
    content.add(Box.createVerticalStrut(12));
    JPanel stages = new JPanel(new GridLayout(1, 2, 12, 0));
    stages.setOpaque(false);
    stages.setAlignmentX(JPanel.LEFT_ALIGNMENT);
    stages.setMaximumSize(new Dimension(Integer.MAX_VALUE, 164));
    stages.add(card("ARQUIVOS, SKILLS, AGENTS E HOOKS", "A CLI pode ler instruções, specs, skills e definições de agentes; pode executar hooks e ferramentas no computador. Somente o contexto que ela realmente enviar entra na contagem do modelo. A presença de um arquivo no projeto não prova seu envio.", INPUT, 5));
    JPanel cacheAndTurns = card("MODELO, CACHE E NOVOS TURNOS", "O IAMonitor não executa o modelo: a CLI usa um serviço remoto da ferramenta ou do provedor. O servidor físico não é identificado. O cache pode reutilizar contexto nesse serviço; não é um arquivo anexado pelo monitor. Respostas e ferramentas locais podem causar novas chamadas até a conclusão.", OUTPUT, 5);
    clickable(cacheAndTurns, () -> openCache.accept(null));
    stages.add(cacheAndTurns);
    content.add(stages);
    content.add(Box.createVerticalStrut(22));

    content.add(heading("Entrada, saída e tráfego de dados"));
    content.add(Box.createVerticalStrut(10));
    FlowDiagram flow = new FlowDiagram(provider, input, output, openStage);
    flow.setAlignmentX(JPanel.LEFT_ALIGNMENT);
    content.add(flow);
    content.add(Box.createVerticalStrut(12));
    content.add(paragraph("As setas mostram o caminho da informação. As barras mostram tokens usados pelo modelo, não bytes de rede ou de download. Parte da saída contada pode não voltar como texto visível.", 12, ModalStyle.MUTED, 2));
    content.add(Box.createVerticalStrut(22));

    content.add(heading("O que significam os números"));
    content.add(Box.createVerticalStrut(10));
    JPanel definitions = new JPanel(new GridLayout(1, 2, 12, 0));
    definitions.setOpaque(false);
    definitions.setAlignmentX(JPanel.LEFT_ALIGNMENT);
    definitions.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
    definitions.add(card("ENTRADA  ·  O MODELO RECEBE", "Seu pedido e o contexto realmente enviado ao modelo: instruções, histórico, trechos de arquivos e resultados de ferramentas. Em várias trocas, o mesmo contexto pode ser contado novamente.", INPUT, 4));
    definitions.add(card("SAÍDA  ·  O MODELO GERA", "O modelo remoto gera a resposta, comandos e chamadas de ferramentas; a parte visível volta à CLI. A contagem pode incluir raciocínio e outros tokens internos que não voltam como texto. Não é volume de download.", OUTPUT, 4));
    content.add(definitions);
    content.add(Box.createVerticalStrut(12));
    JPanel details = new JPanel(new GridLayout(1, 2, 12, 0));
    details.setOpaque(false);
    details.setAlignmentX(JPanel.LEFT_ALIGNMENT);
    details.setMaximumSize(new Dimension(Integer.MAX_VALUE, 143));
    JPanel cacheDefinition = card("CACHE  ·  PARTE DA ENTRADA", "Contexto reaproveitado pela IA, quando a CLI informa leitura de cache. Já integra os tokens de entrada; não se soma novamente. O monitor não vê o conteúdo do cache nem mede sua transmissão.", INPUT, 4);
    clickable(cacheDefinition, () -> openCache.accept(null));
    details.add(cacheDefinition);
    details.add(card("PROCESSAMENTO LOCAL", "A CLI lê o projeto, executa ferramentas e mostra os resultados no computador. O monitor identifica arquivos e estima tokens de alguns textos, mas isso não prova que todo arquivo foi enviado à IA.", OUTPUT, 4));
    content.add(details);
    content.add(Box.createVerticalStrut(22));

    content.add(heading("A conta desta sessão"));
    content.add(Box.createVerticalStrut(10));
    String status = count == null ? "Selecione uma sessão para ver as quantidades reportadas." : count.status();
    String equation = count == null ? "Entrada sem cache + cache + saída = total, quando a CLI fornece dados verificáveis."
        : count.equation();
    String included = count == null ? "Cache e raciocínio são parcelas incluídas, não valores extras." : count.included();
    JPanel account = card("TOKENS PROCESSADOS  ·  " + (count == null ? "—" : count.total()),
        status + "\n" + equation + "\n" + included, INPUT, 3);
    clickable(account, () -> openCache.accept(null));
    content.add(account);
    content.add(Box.createVerticalStrut(12));
    content.add(paragraph("A fonte dos números é o uso informado pela CLI após as chamadas ao modelo. O IAMonitor não intercepta a conexão, não lê credenciais, não mede bytes transferidos e não recebe o saldo ou a cobrança da assinatura.", 12, ModalStyle.MUTED, 3));
    content.add(Box.createVerticalStrut(8));
    content.add(paragraph("Se a CLI não trouxer uso, o total aparece como indisponível; se faltar uma parte, aparece como parcial ou mínimo. Gemini pode informar uma parcela adicional sem detalhamento. Estimativas locais de prompt, skills, agents e hooks não entram no total reportado.", 12, ModalStyle.MUTED, 3));
    content.add(Box.createVerticalStrut(12));
    if (run != null) content.add(card("FERRAMENTA DESTA SESSÃO  ·  " + provider,
        Provider.explanation(run.provider()), INPUT, 5));

    JScrollPane scroll = new JScrollPane(content);
    scroll.setBorder(null);
    scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    scroll.getVerticalScrollBar().setUnitIncrement(18);
    JTabbedPane tabs = new JTabbedPane();
    tabs.setFont(new Font("Segoe UI", Font.BOLD, 14));
    tabs.setUI(new javax.swing.plaf.basic.BasicTabbedPaneUI() {
      @Override protected java.awt.Insets getTabInsets(int placement, int index) {
        return new java.awt.Insets(10, 19, 10, 19);
      }
    });
    tabs.setBackground(ModalStyle.PAPER);
    tabs.setForeground(ModalStyle.INK);
    tabs.setBorder(new EmptyBorder(9, 14, 0, 14));
    tabs.addTab("Fluxo da sessão", scroll);
    JScrollPane methodologyScroll = TokenMethodologyPanel.create(run == null ? "codex" : run.provider());
    tabs.addTab("Como cada IA conta", methodologyScroll);
    tabs.setBackgroundAt(0, ModalStyle.HEADER);
    tabs.setForegroundAt(0, Color.WHITE);
    tabs.setBackgroundAt(1, ModalStyle.PAPER);
    tabs.setForegroundAt(1, ModalStyle.INK);
    tabs.addChangeListener(event -> {
      for (int index = 0; index < tabs.getTabCount(); index++) {
        boolean selected = index == tabs.getSelectedIndex();
        tabs.setBackgroundAt(index, selected ? ModalStyle.HEADER : ModalStyle.PAPER);
        tabs.setForegroundAt(index, selected ? Color.WHITE : ModalStyle.INK);
      }
      if (tabs.getSelectedIndex() == 1)
        javax.swing.SwingUtilities.invokeLater(() -> methodologyScroll.getVerticalScrollBar().setValue(0));
    });
    root.add(tabs, BorderLayout.CENTER);
    JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
    footer.setBackground(BG);
    footer.setBorder(new EmptyBorder(10, 27, 14, 27));
    JButton close = ModalStyle.button("Fechar", true);
    close.setFocusable(false);
    close.addActionListener(event -> dialog.dispose());
    footer.add(close);
    root.add(footer, BorderLayout.SOUTH);
    dialog.setContentPane(root);
    dialog.setSize(1060, 760);
    dialog.setMinimumSize(new Dimension(800, 590));
    dialog.setLocationRelativeTo(owner);
    javax.swing.SwingUtilities.invokeLater(() -> scroll.getVerticalScrollBar().setValue(0));
    ModalStyle.show(owner, dialog);
  }

  private static JLabel heading(String text) {
    JLabel label = label(text, 17, Font.BOLD, ModalStyle.INK);
    label.setAlignmentX(JPanel.LEFT_ALIGNMENT);
    return label;
  }

  private static JLabel label(String text, int size, int style, Color color) {
    JLabel label = new JLabel(text);
    label.setFont(new Font("Segoe UI", style, size));
    label.setForeground(color);
    return label;
  }

  private static JTextArea paragraph(String text, int size, Color color, int rows) {
    JTextArea area = new JTextArea(text, rows, 0);
    area.setEditable(false);
    area.setLineWrap(true);
    area.setWrapStyleWord(true);
    area.setOpaque(false);
    area.setForeground(color);
    area.setFont(new Font("Segoe UI", Font.PLAIN, size));
    area.setPreferredSize(new Dimension(380, rows * 21));
    area.setMaximumSize(new Dimension(Integer.MAX_VALUE, rows * 21));
    area.setAlignmentX(JPanel.LEFT_ALIGNMENT);
    return area;
  }

  private static JPanel card(String title, String body, Color color, int rows) {
    JPanel panel = new JPanel(new BorderLayout(0, 9));
    panel.setBackground(PANEL);
    panel.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(ModalStyle.BORDER),
        new EmptyBorder(14, 17, 12, 17)));
    panel.add(label(title, 12, Font.BOLD, color), BorderLayout.NORTH);
    panel.add(paragraph(body, 13, INK, rows), BorderLayout.CENTER);
    panel.setAlignmentX(JPanel.LEFT_ALIGNMENT);
    return panel;
  }

  private static void clickable(JPanel panel, Runnable action) {
    panel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    panel.setToolTipText("Clique para inspecionar os resultados de cache da sessão.");
    MouseAdapter click = new MouseAdapter() {
      @Override public void mouseClicked(MouseEvent event) { action.run(); }
    };
    panel.addMouseListener(click);
    for (java.awt.Component child : panel.getComponents()) {
      child.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      child.addMouseListener(click);
    }
  }

  private static final class GuideContent extends JPanel implements javax.swing.Scrollable {
    @Override public Dimension getPreferredScrollableViewportSize() { return new Dimension(990, 650); }
    @Override public int getScrollableUnitIncrement(java.awt.Rectangle visible, int orientation, int direction) { return 20; }
    @Override public int getScrollableBlockIncrement(java.awt.Rectangle visible, int orientation, int direction) { return 320; }
    @Override public boolean getScrollableTracksViewportWidth() { return true; }
    @Override public boolean getScrollableTracksViewportHeight() { return false; }
  }

  record JourneyCounts(String input, String cache, String output, String total, String status,
      boolean available) {
    static JourneyCounts from(Store.Run run, TokenAccounting.Assessment count, boolean auditable) {
      if (run == null || count == null || !auditable || "—".equals(count.total()))
        return new JourneyCounts("—", "—", "—", "—", "SEM DADOS", false);
      return new JourneyCounts(NUM.format(run.inputTokens()), NUM.format(run.cachedTokens()),
          NUM.format(run.outputTokens()), count.total(), count.status().split(" · ", 2)[0], true);
    }
  }

  static final class JourneyDiagram extends JPanel {
    private final String provider;
    private final JourneyCounts counts;
    private final IntConsumer onClick;

    JourneyDiagram(String provider, JourneyCounts counts) {
      this(provider, counts, stage -> {});
    }

    JourneyDiagram(String provider, JourneyCounts counts, IntConsumer onClick) {
      this.provider = provider;
      this.counts = counts;
      this.onClick = onClick;
      addMouseListener(new MouseAdapter() {
        @Override public void mouseClicked(MouseEvent event) {
          int stage = stageAt(event.getX(), event.getY());
          if (stage != 0) JourneyDiagram.this.onClick.accept(stage);
        }
      });
      addMouseMotionListener(new MouseAdapter() {
        @Override public void mouseMoved(MouseEvent event) {
          setCursor(Cursor.getPredefinedCursor(stageAt(event.getX(), event.getY()) == 0
              ? Cursor.DEFAULT_CURSOR : Cursor.HAND_CURSOR));
        }
      });
      setToolTipText("Clique em um bloco para ver os detalhes da sessão");
      setBackground(PANEL);
      setBorder(BorderFactory.createLineBorder(new Color(52, 84, 99)));
      setPreferredSize(new Dimension(990, 520));
      setMaximumSize(new Dimension(Integer.MAX_VALUE, 520));
    }

    @Override protected void paintComponent(Graphics graphics) {
      super.paintComponent(graphics);
      Graphics2D g = (Graphics2D) graphics.create();
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.scale(getWidth() / 990.0, getHeight() / 520.0);
      text(g, "NO SEU COMPUTADOR", 25, 27, INPUT, 11, Font.BOLD);
      text(g, "CLIQUE NOS BLOCOS PARA VER DETALHES", 266, 27, MUTED, 11, Font.BOLD);
      text(g, "REDE / SERVIÇO REMOTO", 758, 27, INPUT, 11, Font.BOLD);
      stage(g, 1, 24, 43, "PEDIDO", "Prompt escrito", "inicia a sessão",
          "TOKENS: não separados", INPUT, 0, false);
      stage(g, 2, 268, 43, "CLI LOCAL", "Roda na pasta", "e usa seu login",
          "TOKENS: não medidos", INPUT, 1, false);
      stage(g, 3, 512, 43, "CONTEXTO", "Arquivos, skills,", "agents, instruções*",
          "TOKENS: não separados", INPUT, 2, false);
      stage(g, 4, 756, 43, "ENVIO", "CLI envia dados", "ao serviço de IA",
          "ENTRADA*: " + counts.input(), INPUT, 3, counts.available());
      link(g, 234, 268, 118, INPUT);
      link(g, 478, 512, 118, INPUT);
      link(g, 722, 756, 118, INPUT);
      g.setColor(INPUT);
      g.setStroke(new BasicStroke(2.5f));
      g.drawLine(965, 198, 965, 302);
      triangle(g, 965, 305, 0, 1, INPUT);

      g.setColor(new Color(17, 42, 55));
      g.fillRoundRect(671, 219, 266, 64, 12, 12);
      g.setColor(new Color(68, 106, 119));
      g.drawRoundRect(671, 219, 266, 64, 12, 12);
      icon(g, 4, 688, 237, INPUT);
      text(g, "CACHE*: " + counts.cache() + (counts.available() ? " tokens" : ""),
          723, 244, INPUT, 11, Font.BOLD);
      text(g, "Já incluído na entrada do modelo", 723, 267, MUTED, 11, Font.PLAIN);
      g.setColor(new Color(104, 151, 161));
      g.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1,
          new float[]{5, 5}, 0));
      g.drawLine(846, 283, 846, 303);
      triangle(g, 846, 304, 0, 1, new Color(104, 151, 161));
      g.setStroke(new BasicStroke(2));

      stage(g, 8, 24, 305, "CONCLUSÃO", "Resultado exibido", "e uso registrado",
          counts.status() + ": " + counts.total(), OUTPUT, 7, counts.available());
      stage(g, 7, 268, 305, "AÇÕES LOCAIS", "Ferramentas / hooks", "quando acionados",
          "TOKENS: não medidos", OUTPUT, 6, false);
      stage(g, 6, 512, 305, "RETORNO", "Resposta visível", "volta à CLI",
          "SAÍDA GERADA*: " + counts.output(), OUTPUT, 5, counts.available());
      stage(g, 5, 756, 305, "MODELO REMOTO", provider, "processa o pedido",
          "TOTAL*: " + counts.total(), OUTPUT, 4, counts.available());
      link(g, 756, 722, 380, OUTPUT);
      link(g, 512, 478, 380, OUTPUT);
      link(g, 268, 234, 380, OUTPUT);
      g.setColor(new Color(104, 151, 161));
      g.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1,
          new float[]{6, 6}, 0));
      g.drawLine(375, 304, 375, 235);
      g.drawLine(375, 235, 617, 235);
      g.drawLine(617, 235, 617, 199);
      triangle(g, 617, 198, 0, -1, new Color(104, 151, 161));
      text(g, "ciclos observados acima", 392, 224, MUTED, 11, Font.PLAIN);
      text(g, "* Totais reportados da sessão; não somar as etapas entre si. Cache já integra a entrada.",
          26, 485, MUTED, 12, Font.PLAIN);
      text(g, "Clique em qualquer bloco para ver os detalhes. Tokens não representam tráfego de rede.",
          26, 505, MUTED, 12, Font.PLAIN);
      g.dispose();
    }

    JourneyCounts counts() { return counts; }

    int stageAt(int pixelX, int pixelY) {
      if (getWidth() <= 0 || getHeight() <= 0) return 0;
      double x = pixelX * 990.0 / getWidth();
      double y = pixelY * 520.0 / getHeight();
      if (x >= 671 && x <= 937 && y >= 219 && y <= 283) return 9;
      int[] xs = {24, 268, 512, 756};
      for (int i = 0; i < xs.length; i++) {
        if (x < xs[i] || x > xs[i] + 210) continue;
        if (y >= 43 && y <= 198) return i + 1;
        if (y >= 305 && y <= 460) return 8 - i;
      }
      return 0;
    }

    private void stage(Graphics2D g, int number, int x, int y, String title,
        String line1, String line2, String metric, Color accent, int kind, boolean measured) {
      g.setColor(new Color(17, 42, 55));
      g.fillRoundRect(x, y, 210, 155, 14, 14);
      g.setColor(new Color(52, 84, 99));
      g.drawRoundRect(x, y, 210, 155, 14, 14);
      g.setColor(accent);
      g.fillOval(x + 15, y + 15, 27, 27);
      text(g, String.valueOf(number), x + 24, y + 34, BG, 13, Font.BOLD);
      icon(g, kind, x + 171, y + 17, accent);
      text(g, title, x + 15, y + 63, accent, 12, Font.BOLD);
      text(g, line1, x + 15, y + 89, INK, 12, Font.PLAIN);
      text(g, line2, x + 15, y + 110, MUTED, 11, Font.PLAIN);
      g.setColor(measured ? new Color(39, 75, 83) : new Color(27, 52, 62));
      g.fillRoundRect(x + 10, y + 121, 190, 25, 7, 7);
      text(g, metric, x + 17, y + 138, measured ? accent : MUTED, 11, Font.BOLD);
    }

    private void icon(Graphics2D g, int kind, int x, int y, Color color) {
      g.setColor(color);
      g.setStroke(new BasicStroke(2));
      switch (kind) {
        case 0, 5 -> {
          g.drawRoundRect(x, y, 23, 17, 5, 5);
          g.drawLine(x + 6, y + 17, x + 3, y + 22);
        }
        case 1 -> {
          g.drawRoundRect(x, y, 25, 18, 3, 3);
          g.drawLine(x + 3, y + 23, x + 23, y + 23);
          g.drawLine(x + 5, y + 8, x + 9, y + 11);
          g.drawLine(x + 9, y + 11, x + 5, y + 14);
        }
        case 2 -> {
          g.drawRect(x + 5, y, 18, 22);
          g.drawRect(x, y + 5, 18, 22);
          g.drawLine(x + 4, y + 14, x + 13, y + 14);
        }
        case 3 -> {
          g.drawLine(x, y + 13, x + 24, y + 13);
          triangle(g, x + 25, y + 13, 1, 0, color);
        }
        case 4 -> {
          g.drawRoundRect(x + 3, y + 3, 20, 20, 4, 4);
          g.drawRect(x + 9, y + 9, 8, 8);
        }
        case 6 -> {
          g.drawOval(x + 3, y + 3, 20, 20);
          g.drawLine(x + 1, y + 13, x + 25, y + 13);
          g.drawLine(x + 13, y + 1, x + 13, y + 25);
        }
        case 7 -> {
          g.drawOval(x + 1, y + 1, 25, 25);
          g.drawLine(x + 7, y + 14, x + 12, y + 19);
          g.drawLine(x + 12, y + 19, x + 21, y + 8);
        }
        default -> {
          g.drawOval(x + 2, y + 6, 23, 9);
          g.drawLine(x + 2, y + 10, x + 2, y + 22);
          g.drawLine(x + 25, y + 10, x + 25, y + 22);
          g.drawArc(x + 2, y + 17, 23, 9, 180, 180);
        }
      }
    }

    private void link(Graphics2D g, int from, int to, int y, Color color) {
      g.setColor(color);
      g.setStroke(new BasicStroke(2.5f));
      g.drawLine(from, y, to, y);
      triangle(g, to, y, to > from ? 1 : -1, 0, color);
    }

    private void triangle(Graphics2D g, int x, int y, int dx, int dy, Color color) {
      Path2D path = new Path2D.Double();
      if (dx != 0) {
        path.moveTo(x, y);
        path.lineTo(x - dx * 8, y - 5);
        path.lineTo(x - dx * 8, y + 5);
      } else {
        path.moveTo(x, y);
        path.lineTo(x - 5, y - dy * 8);
        path.lineTo(x + 5, y - dy * 8);
      }
      path.closePath();
      g.setColor(color);
      g.fill(path);
    }

    private void text(Graphics2D g, String value, int x, int y, Color color, int size, int style) {
      g.setColor(color);
      g.setFont(new Font("Segoe UI", style, size));
      g.drawString(value, x, y);
    }
  }

  static final class FlowDiagram extends JPanel {
    private final String provider;
    private final long input;
    private final long output;
    private final IntConsumer onClick;

    FlowDiagram(String provider, long input, long output) {
      this(provider, input, output, stage -> {});
    }

    FlowDiagram(String provider, long input, long output, IntConsumer onClick) {
      this.provider = provider;
      this.input = input;
      this.output = output;
      this.onClick = onClick;
      addMouseListener(new MouseAdapter() {
        @Override public void mouseClicked(MouseEvent event) {
          int stage = stageAt(event.getX(), event.getY());
          if (stage != 0) FlowDiagram.this.onClick.accept(stage);
        }
      });
      addMouseMotionListener(new MouseAdapter() {
        @Override public void mouseMoved(MouseEvent event) {
          setCursor(Cursor.getPredefinedCursor(stageAt(event.getX(), event.getY()) == 0
              ? Cursor.DEFAULT_CURSOR : Cursor.HAND_CURSOR));
        }
      });
      setToolTipText("Clique em um bloco ou barra para ver os detalhes da sessão");
      setBackground(PANEL);
      setBorder(BorderFactory.createLineBorder(new Color(52, 84, 99)));
      setPreferredSize(new Dimension(990, 338));
      setMaximumSize(new Dimension(Integer.MAX_VALUE, 338));
    }

    int stageAt(int pixelX, int pixelY) {
      if (getWidth() <= 0 || getHeight() <= 0) return 0;
      double x = pixelX * 990.0 / getWidth();
      double y = pixelY * 338.0 / getHeight();
      if (y >= 16 && y <= 146) {
        if (x >= 18 && x <= 304) return 2;
        if (x >= 352 && x <= 638) return 4;
        if (x >= 686 && x <= 972) return 5;
      }
      if (x >= 24 && x <= 972 && y >= 252 && y <= 279) return 4;
      if (x >= 24 && x <= 972 && y >= 287 && y <= 314) return 6;
      return 0;
    }
    @Override protected void paintComponent(Graphics graphics) {
      super.paintComponent(graphics);
      Graphics2D g = (Graphics2D) graphics.create();
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.scale(getWidth() / 990.0, getHeight() / 338.0);
      zone(g, 18, "SEU COMPUTADOR", "Projeto + CLI + IAMonitor",
          "Leitura de arquivos e ferramentas", "Estimativas locais separadas");
      zone(g, 352, "REDE", "Dados trafegam entre as partes",
          "Bytes e pacotes não medidos", "Token não é unidade de rede");
      zone(g, 686, "SERVIDOR DA IA", provider,
          "Modelo processa a entrada", "Resposta visível retorna à CLI");

      arrow(g, 40, 950, 184, INPUT);
      draw(g, "ENTRADA  •  conteúdo enviado ao modelo", 42, 177, INPUT, 13, Font.BOLD);
      arrow(g, 950, 40, 226, OUTPUT);
      draw(g, "RESPOSTA VISÍVEL  •  volta à CLI", 600, 219, OUTPUT, 13, Font.BOLD);

      long biggest = Math.max(input, output);
      bar(g, 273, INPUT, "Entrada  ·  IA recebeu", input, biggest);
      bar(g, 308, OUTPUT, "Saída  ·  modelo gerou", output, biggest);
      g.dispose();
    }

    private void zone(Graphics2D g, int x, String title, String line1, String line2, String line3) {
      g.setColor(new Color(17, 42, 55));
      g.fillRoundRect(x, 16, 286, 130, 16, 16);
      g.setColor(new Color(52, 84, 99));
      g.drawRoundRect(x, 16, 286, 130, 16, 16);
      draw(g, title, x + 16, 43, INPUT, 12, Font.BOLD);
      draw(g, line1, x + 16, 72, INK, 14, Font.BOLD);
      draw(g, line2, x + 16, 101, MUTED, 12, Font.PLAIN);
      draw(g, line3, x + 16, 122, MUTED, 12, Font.PLAIN);
    }

    private void arrow(Graphics2D g, int start, int end, int y, Color color) {
      g.setColor(color);
      g.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
      g.drawLine(start, y, end, y);
      int direction = end > start ? 1 : -1;
      Path2D triangle = new Path2D.Double();
      triangle.moveTo(end, y);
      triangle.lineTo(end - direction * 13, y - 7);
      triangle.lineTo(end - direction * 13, y + 7);
      triangle.closePath();
      g.fill(triangle);
    }

    private void bar(Graphics2D g, int y, Color color, String name, long amount, long biggest) {
      draw(g, name, 42, y, INK, 12, Font.PLAIN);
      g.setColor(new Color(17, 42, 55));
      g.fillRoundRect(242, y - 14, 590, 18, 9, 9);
      if (amount >= 0 && biggest > 0) {
        int width = (int) Math.max(amount > 0 ? 3 : 0, Math.round(590.0 * amount / biggest));
        g.setColor(color);
        g.fillRoundRect(242, y - 14, width, 18, 9, 9);
      }
      draw(g, amount < 0 ? "não informado" : NUM.format(amount) + " tokens",
          845, y, amount < 0 ? MUTED : color, 12, Font.BOLD);
    }

    private void draw(Graphics2D g, String text, int x, int y, Color color, int size, int style) {
      g.setColor(color);
      g.setFont(new Font("Segoe UI", style, size));
      g.drawString(text, x, y);
    }
  }
}

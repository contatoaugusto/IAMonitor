package br.iamonitor.desktop;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Locale;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JToggleButton;
import javax.swing.JTabbedPane;
import javax.swing.JPopupMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.plaf.basic.BasicTabbedPaneUI;

final class Dashboard extends JFrame {
  private static final Color BG = new Color(16, 36, 49);
  private static final Color PANEL = new Color(25, 54, 68);
  private static final Color INK = new Color(244, 242, 236);
  private static final Color MUTED = new Color(183, 201, 209);
  private static final Color ACCENT = new Color(128, 184, 220);
  private static final Color TOKEN_INPUT = new Color(145, 211, 240);
  private static final Color TOKEN_OUTPUT = new Color(250, 186, 119);
  private static final Color TOKEN_CACHE = new Color(202, 181, 241);
  private static final NumberFormat NUM = NumberFormat.getIntegerInstance(Locale.forLanguageTag("pt-BR"));
  private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM HH:mm:ss")
      .withZone(ZoneId.systemDefault());
  private final Store store;
  private final Branding branding;
  private final CodexRunner runner;
  private JPanel sidebarPanel;
  private final JButton presentationButton = button("Apresentar");
  private final DefaultListModel<Store.Run> runModel = new DefaultListModel<>();
  private final JList<Store.Run> runList = new JList<>(runModel);
  private final JLabel title = new JLabel("Nenhuma sessão selecionada");
  private final JLabel subtitle = new JLabel("Inicie uma sessão para acompanhar sua IA.");
  private final JButton stopButton = button("Interromper");
  private final JButton rawButton = button("Eventos JSONL");
  private final JButton pdfButton = button("Relatório PDF");
  private final JButton restartButton = button("Zerar e executar");
  private final JButton deleteButton = button("Excluir sessão");
  private final JLabel liveState = label("Aguardando sessão", 17, Font.BOLD, INK);
  private final JLabel promptPreview = label("Pedido: —", 12, Font.PLAIN, INK);
  private final Map<String, JToggleButton> simulationButtons = new LinkedHashMap<>();
  private final JLabel simulatedTotal = value();
  private final JLabel simulatedInput = value();
  private final JLabel simulatedSavings = value();
  private final JLabel simulationNote = label("Selecione uma sessão.", 11, Font.PLAIN, MUTED);
  private String simulationSessionId = "";
  private final JLabel liveCount = label("0 operações · 0 arquivos", 11, Font.PLAIN, MUTED);
  private final JLabel liveTotal = value();
  private final JLabel liveTotalState = label("AGUARDANDO", 11, Font.BOLD, MUTED);
  private final JLabel liveTotalEquation = label("", 12, Font.BOLD, INK);
  private final JLabel quotaTitle = label("ASSINATURA · SELECIONE UMA SESSÃO", 12, Font.BOLD, TOKEN_CACHE);
  private final JLabel quotaHeadline = label("Cota: —", 14, Font.BOLD, INK);
  private final JLabel quotaDetail = label("", 11, Font.PLAIN, INK);
  private final JLabel quotaNote = label("", 10, Font.PLAIN, MUTED);
  private final JButton quotaSource = button("Consultar fonte oficial");
  private final DefaultTableModel liveEventModel = model("Horário", "Operação", "Detalhe observado", "Entrada IA", "Cache", "Saída IA");
  private final DefaultTableModel liveSourceModel = model("Origem", "Tokens", "Base");
  private final JTable liveEventTable = table(liveEventModel);
  private final JTable liveSourceTable = table(liveSourceModel);
  private JTabbedPane workTabs;
  private final WorkbenchCharts.Usage usageChart = new WorkbenchCharts.Usage();
  private final WorkbenchCharts.Artifacts artifactChart = new WorkbenchCharts.Artifacts();
  private final JLabel[] recentEvents = new JLabel[3];
  private JScrollPane liveSourceScroll;
  private final JPanel executionHero = panel();
  private final JLabel executionPhase = label("INICIANDO", 11, Font.BOLD, TOKEN_OUTPUT);
  private final JLabel executionHeadline = label("Conectando à CLI", 21, Font.BOLD, INK);
  private final JLabel executionDetail = label("Aguardando o primeiro evento da IA...", 13, Font.PLAIN, INK);
  private final JLabel executionMeta = label("00:00:00 · 0 eventos · 0 arquivos", 11, Font.PLAIN, MUTED);
  private final JProgressBar executionPulse = new JProgressBar();
  private final Timer refreshTimer;
  private Store.Detail current;
  private List<Store.QuotaSnapshot> currentQuota = List.of();
  private boolean applying;
  private boolean updatingSimulation;
  private boolean loading;
  private boolean refreshPending;
  private boolean sessionStarting;
  private String selectedId = "";

  Dashboard(Store store) {
    this(store, null);
  }

  Dashboard(Store store, List<String> codexCommand) {
    super("IAMonitor Desktop");
    this.store = store;
    this.branding = Branding.load();
    setTitle(branding.name() + " · IAMonitor Desktop");
    setJMenuBar(helpMenu());
    if (branding.logo() != null) setIconImage(branding.logo());
    this.runner = new CodexRunner(store, this::requestRefresh, codexCommand);
    setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    setMinimumSize(new Dimension(1100, 720));
    setSize(1400, 900);
    setLocationRelativeTo(null);
    getContentPane().setBackground(BG);
    setLayout(new BorderLayout());
    sidebarPanel = sidebar();
    add(sidebarPanel, BorderLayout.WEST);
    add(mainPanel(), BorderLayout.CENTER);
    stopButton.setVisible(false);
    runList.addListSelectionListener(event -> {
      if (!event.getValueIsAdjusting() && !applying) {
        Store.Run selected = runList.getSelectedValue();
        selectedId = selected == null ? "" : selected.id();
        requestRefresh();
      }
    });
    stopButton.addActionListener(event -> { if (current != null) runner.stop(current.run().id()); });
    rawButton.addActionListener(event -> exportRaw());
    pdfButton.addActionListener(event -> exportPdf());
    restartButton.addActionListener(event -> restartSession());
    deleteButton.addActionListener(event -> deleteSession());
    registerSourceDetails(liveSourceTable);
    artifactChart.onCategorySelected(this::showArtifactCategory);
    usageChart.onCacheSelected(this::showCacheDetails);
    liveTotalEquation.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    liveTotalEquation.setToolTipText("Clique para inspecionar o cache e os resultados de uso da sessão.");
    liveTotalEquation.addMouseListener(new MouseAdapter() {
      @Override public void mouseClicked(MouseEvent event) { showCacheDetails(null); }
    });
    liveEventTable.addMouseListener(new MouseAdapter() {
      @Override public void mouseClicked(MouseEvent event) {
        if (current == null || liveEventTable.columnAtPoint(event.getPoint()) < 0) return;
        int column = liveEventTable.convertColumnIndexToModel(
            liveEventTable.columnAtPoint(event.getPoint()));
        if (column != 4) return;
        int row = liveEventTable.rowAtPoint(event.getPoint());
        if (row < 0) return;
        Store.Event selected = current.events().get(liveEventTable.convertRowIndexToModel(row));
        showCacheDetails("turn.completed".equals(selected.type()) ? selected.seq() : null);
      }
    });
    liveEventTable.getTableHeader().addMouseListener(new MouseAdapter() {
      @Override public void mouseClicked(MouseEvent event) {
        int column = liveEventTable.getTableHeader().columnAtPoint(event.getPoint());
        if (column >= 0 && liveEventTable.convertColumnIndexToModel(column) == 4)
          showCacheDetails(null);
      }
    });
    addWindowListener(new WindowAdapter() {
      @Override public void windowClosing(WindowEvent event) {
        if (runner.activeCount() > 0 && JOptionPane.showConfirmDialog(Dashboard.this,
            "Interromper as sessões em andamento e fechar?", "Sair", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        refreshTimer.stop();
        runner.close();
        try { store.close(); } catch (SQLException ignored) {}
        dispose();
      }
    });
    refreshTimer = new Timer(1500, event -> requestRefresh());
    refreshTimer.start();
    requestRefresh();
  }

  private JMenuBar helpMenu() {
    JMenuBar bar = new JMenuBar();
    bar.setBackground(new Color(243, 238, 227));
    bar.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, ModalStyle.BORDER));
    JMenu help = new JMenu("Ajuda");
    help.setFont(new Font("Segoe UI", Font.BOLD, 13));
    help.setForeground(ModalStyle.INK);
    JMenuItem measurement = new JMenuItem("Como são medidos os tokens...");
    measurement.addActionListener(event -> showUsageHelp());
    JMenuItem about = new JMenuItem("Sobre o IAMonitor...");
    about.addActionListener(event -> showAbout());
    help.add(measurement);
    help.addSeparator();
    help.add(about);
    bar.add(help);
    return bar;
  }

  private JPanel sidebar() {
    JPanel side = panel();
    side.setPreferredSize(new Dimension(265, 0));
    side.setLayout(new BorderLayout(0, 18));
    side.setBorder(new EmptyBorder(22, 18, 18, 16));
    JLabel brand = label("<html>" + escape(branding.name()) + "</html>", 22, Font.BOLD, ACCENT);
    JPanel identity = panel();
    identity.setLayout(new BorderLayout(10, 0));
    if (branding.logo() != null)
      identity.add(new JLabel(new ImageIcon(branding.logo().getScaledInstance(52, 52, Image.SCALE_SMOOTH))), BorderLayout.WEST);
    identity.add(brand, BorderLayout.CENTER);
    JButton newButton = button("+ Nova sessão");
    newButton.setBackground(ACCENT);
    newButton.setForeground(BG);
    newButton.addActionListener(event -> newSession());
    JPanel top = panel();
    top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
    identity.setAlignmentX(LEFT_ALIGNMENT);
    newButton.setAlignmentX(LEFT_ALIGNMENT);
    top.add(identity); top.add(Box.createVerticalStrut(5));
    top.add(label("<html>" + escape(branding.subtitle()) + "</html>", 11, Font.PLAIN, MUTED));
    top.add(Box.createVerticalStrut(22)); top.add(newButton);
    side.add(top, BorderLayout.NORTH);
    runList.setBackground(PANEL);
    runList.setForeground(INK);
    runList.setSelectionBackground(new Color(32, 74, 84));
    runList.setFixedCellHeight(52);
    runList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    runList.setCellRenderer(new DefaultListCellRenderer() {
      @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
        JLabel cell = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focus);
        Store.Run run = (Store.Run) value;
        cell.setText("<html><b>" + escape(Path.of(run.project()).getFileName().toString()) + "</b><br><span style='color:#91aab9'>"
            + escape(Provider.name(run.provider())) + " · " + escape(statusName(run.status())) + "</span></html>");
        cell.setBorder(new EmptyBorder(5, 10, 5, 6));
        cell.setBackground(selected ? new Color(32, 74, 84) : PANEL);
        cell.setForeground(INK);
        return cell;
      }
    });
    JPanel history = panel();
    history.setLayout(new BorderLayout(0, 8));
    history.add(label("SESSÕES RECENTES", 11, Font.BOLD, MUTED), BorderLayout.NORTH);
    history.add(new JScrollPane(runList), BorderLayout.CENTER);
    side.add(history, BorderLayout.CENTER);
    JPanel footer = panel();
    footer.setLayout(new BorderLayout(0, 8));
    footer.add(quotaPanel(), BorderLayout.CENTER);
    footer.add(label("Dados locais · SQLite em data/", 10, Font.PLAIN, MUTED), BorderLayout.SOUTH);
    side.add(footer, BorderLayout.SOUTH);
    return side;
  }

  private JPanel mainPanel() {
    JPanel main = panel();
    main.setLayout(new BorderLayout(0, 8));
    main.setBorder(new EmptyBorder(11, 20, 11, 20));
    JPanel head = panel();
    head.setLayout(new BorderLayout(12, 8));
    JPanel heading = panel();
    heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
    title.setFont(new Font("Segoe UI", Font.BOLD, 23)); title.setForeground(INK);
    subtitle.setForeground(MUTED);
    heading.add(title); heading.add(Box.createVerticalStrut(3)); heading.add(subtitle);
    heading.setOpaque(false);
    JPanel headingRow = panel();
    headingRow.setLayout(new BorderLayout());
    headingRow.setBorder(new EmptyBorder(0, 0, 6, 0));
    headingRow.add(heading, BorderLayout.CENTER);
    head.add(headingRow, BorderLayout.NORTH);
    JPanel actions = panel();
    actions.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 3));
    actions.add(stopButton);
    actions.add(pdfButton);
    presentationButton.addActionListener(event -> {
      boolean presenting = sidebarPanel.isVisible();
      sidebarPanel.setVisible(!presenting);
      presentationButton.setText(presenting ? "Sair da apresentação" : "Apresentar");
      workTabs.setSelectedIndex(0);
      revalidate();
      repaint();
    });
    actions.add(presentationButton);
    JButton more = button("Mais ações ▾");
    JPopupMenu menu = new JPopupMenu();
    for (JButton action : List.of(restartButton, rawButton, deleteButton)) {
      JMenuItem item = new JMenuItem(action.getText());
      item.addActionListener(event -> action.doClick());
      menu.add(item);
    }
    menu.addSeparator();
    JMenuItem trace = new JMenuItem("Dados do projeto e envio remoto...");
    trace.addActionListener(event -> showDataTrace());
    menu.add(trace);
    JMenuItem help = new JMenuItem("Como são medidos os tokens?");
    help.addActionListener(event -> showUsageHelp());
    menu.add(help);
    JMenuItem about = new JMenuItem("Sobre o app");
    about.addActionListener(event -> showAbout());
    menu.add(about);
    more.addActionListener(event -> {
      for (int i = 0; i < 3; i++) menu.getComponent(i).setEnabled(
          List.of(restartButton, rawButton, deleteButton).get(i).isEnabled());
      menu.show(more, 0, more.getHeight());
    });
    actions.add(more);
    head.add(actions, BorderLayout.SOUTH);
    main.add(head, BorderLayout.NORTH);
    main.add(livePanel(), BorderLayout.CENTER);
    return main;
  }

  private JPanel livePanel() {
    JPanel body = panel();
    body.setLayout(new BorderLayout());
    body.setBorder(new EmptyBorder(0, 12, 7, 12));
    JTabbedPane tabs = new JTabbedPane();
    workTabs = tabs;
    tabs.setUI(new BasicTabbedPaneUI() {
      @Override protected void paintContentBorder(Graphics graphics, int placement, int selected) {}
      @Override protected void paintTabBorder(Graphics graphics, int placement, int index,
          int x, int y, int width, int height, boolean selected) {}
      @Override protected void paintTabBackground(Graphics graphics, int placement, int index,
          int x, int y, int width, int height, boolean selected) {
        graphics.setColor(selected ? PANEL : new Color(32, 68, 82));
        graphics.fillRect(x, y, width, height);
      }
    });
    tabs.setFont(new Font("Segoe UI", Font.BOLD, 13));
    tabs.setBackground(BG);
    tabs.setForeground(INK);
    JPanel overview = panel();
    overview.setLayout(new BorderLayout(0, 10));
    overview.setBorder(new EmptyBorder(10, 0, 0, 0));
    JPanel top = panel();
    top.setLayout(new BorderLayout(0, 9));
    JPanel info = panel();
    info.setLayout(new GridLayout(2, 1, 0, 3));
    JPanel liveHeading = panel();
    liveHeading.setLayout(new BorderLayout());
    liveHeading.add(label("VISÃO GERAL DA SESSÃO", 11, Font.BOLD, ACCENT), BorderLayout.WEST);
    liveHeading.add(liveState, BorderLayout.EAST);
    info.add(liveHeading);
    info.add(promptPreview);
    top.add(info, BorderLayout.NORTH);
    top.add(totalPanel(liveTotal, liveTotalState, liveTotalEquation), BorderLayout.SOUTH);
    overview.add(top, BorderLayout.NORTH);
    JPanel charts = panel();
    charts.setLayout(new GridLayout(1, 2, 10, 0));
    charts.add(usageChart);
    charts.add(artifactChart);
    overview.add(charts, BorderLayout.CENTER);
    JPanel activity = panel();
    activity.setLayout(new BorderLayout(0, 6));
    JPanel activityHeader = panel();
    activityHeader.setLayout(new BorderLayout(0, 5));
    activityHeader.add(label("OPERAÇÃO ATUAL E REGISTRO RECENTE", 12, Font.BOLD, INK), BorderLayout.NORTH);
    executionHero.setLayout(new BorderLayout(0, 9));
    executionHero.setPreferredSize(new Dimension(100, 134));
    executionHero.setBackground(new Color(27, 68, 76));
    executionHero.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(new Color(91, 174, 180), 2), new EmptyBorder(10, 18, 9, 18)));
    JPanel executionText = new JPanel(new GridLayout(4, 1, 0, 5));
    executionText.setOpaque(false);
    executionText.add(executionPhase); executionText.add(executionHeadline);
    executionText.add(executionDetail); executionText.add(executionMeta);
    executionHero.add(executionText, BorderLayout.CENTER);
    executionPulse.setIndeterminate(true);
    executionPulse.setForeground(new Color(115, 209, 195));
    executionPulse.setBackground(new Color(25, 54, 68));
    executionPulse.setBorderPainted(false);
    executionPulse.setPreferredSize(new Dimension(100, 5));
    executionHero.add(executionPulse, BorderLayout.SOUTH);
    activityHeader.add(executionHero, BorderLayout.CENTER);
    activity.add(activityHeader, BorderLayout.NORTH);
    JPanel recent = panel();
    recent.setBackground(PANEL);
    recent.setLayout(new GridLayout(3, 1, 0, 1));
    recent.setBorder(new EmptyBorder(6, 12, 6, 12));
    for (int i = 0; i < recentEvents.length; i++) {
      recentEvents[i] = label("", 12, Font.PLAIN, i == 0 ? INK : MUTED);
      recent.add(recentEvents[i]);
    }
    activity.add(recent, BorderLayout.CENTER);
    activity.add(liveCount, BorderLayout.SOUTH);
    overview.add(activity, BorderLayout.SOUTH);

    JPanel details = panel();
    details.setLayout(new BorderLayout(0, 10));
    details.setBorder(new EmptyBorder(10, 0, 0, 0));
    details.add(simulationPanel(), BorderLayout.NORTH);
    JPanel detailTables = panel();
    detailTables.setLayout(new BorderLayout(0, 8));
    JPanel eventsHeading = panel();
    eventsHeading.setLayout(new BorderLayout(8, 0));
    eventsHeading.add(label("TODOS OS EVENTOS OBSERVADOS", 12, Font.BOLD, INK), BorderLayout.WEST);
    JButton traceButton = button("Dados e envio");
    traceButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
    traceButton.setToolTipText("Evidências locais e limite do envio remoto");
    traceButton.addActionListener(event -> showDataTrace());
    eventsHeading.add(traceButton, BorderLayout.EAST);
    detailTables.add(eventsHeading, BorderLayout.NORTH);
    JTable operations = liveEventTable;
    operations.getColumnModel().getColumn(0).setPreferredWidth(85);
    operations.getColumnModel().getColumn(1).setPreferredWidth(140);
    operations.getColumnModel().getColumn(2).setPreferredWidth(250);
    operations.getColumnModel().getColumn(3).setPreferredWidth(85);
    operations.getColumnModel().getColumn(4).setPreferredWidth(65);
    operations.getColumnModel().getColumn(5).setPreferredWidth(75);
    operations.getTableHeader().setToolTipText("Recebeu = tokens que o modelo remoto recebeu; cache já integra a entrada; gerou = tokens que o modelo produziu, não bytes de download.");
    detailTables.add(new JScrollPane(operations), BorderLayout.CENTER);

    JPanel sources = panel();
    sources.setLayout(new BorderLayout(0, 7));
    sources.add(label("ORIGENS E CACHE · CLIQUE PARA DETALHES", 11, Font.BOLD, INK), BorderLayout.NORTH);
    liveSourceTable.setRowHeight(24);
    liveSourceTable.getTableHeader().setPreferredSize(new Dimension(0, 30));
    liveSourceScroll = new JScrollPane(liveSourceTable);
    liveSourceScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
    liveSourceScroll.setPreferredSize(new Dimension(200, 30 + 5 * 24 + 3));
    sources.add(liveSourceScroll, BorderLayout.CENTER);
    JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, detailTables, sources);
    split.setBorder(null);
    split.setResizeWeight(0.69);
    split.setDividerLocation(0.69);
    SwingUtilities.invokeLater(() -> split.setDividerLocation(0.69));
    details.add(split, BorderLayout.CENTER);
    tabs.addTab("Visão geral", overview);
    tabs.addTab("Explorar dados", details);
    body.add(tabs, BorderLayout.CENTER);
    return body;
  }

  private JPanel quotaPanel() {
    JPanel box = panel();
    box.setLayout(new BorderLayout(0, 8));
    box.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(TOKEN_CACHE, 1),
        new EmptyBorder(7, 12, 7, 12)));
    box.setPreferredSize(new Dimension(220, 280));
    box.setAlignmentX(LEFT_ALIGNMENT);
    JPanel lines = panel();
    lines.setLayout(new BoxLayout(lines, BoxLayout.Y_AXIS));
    quotaTitle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
    quotaHeadline.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
    quotaDetail.setMaximumSize(new Dimension(Integer.MAX_VALUE, 85));
    quotaNote.setMaximumSize(new Dimension(Integer.MAX_VALUE, 65));
    lines.add(quotaTitle);
    lines.add(Box.createVerticalStrut(8));
    lines.add(quotaHeadline);
    lines.add(Box.createVerticalStrut(8));
    lines.add(quotaDetail);
    lines.add(Box.createVerticalStrut(8));
    lines.add(quotaNote);
    box.add(lines, BorderLayout.CENTER);
    quotaSource.setEnabled(false);
    quotaSource.addActionListener(event -> {
      if (current == null) return;
      try {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
          Desktop.getDesktop().browse(URI.create(quotaUrl(current.run().provider())));
      } catch (Exception error) {
        JOptionPane.showMessageDialog(this, "Não foi possível abrir a consulta oficial.",
            "Navegador indisponível", JOptionPane.WARNING_MESSAGE);
      }
    });
    box.add(quotaSource, BorderLayout.SOUTH);
    return box;
  }

  private static String quotaUrl(String provider) {
    return switch (provider) {
      case "codex" -> "https://chatgpt.com/codex/cloud/settings/usage";
      case "claude" -> "https://support.claude.com/en/articles/11647753-how-do-usage-and-length-limits-work";
      case "gemini" -> "https://geminicli.com/docs/resources/quota-and-pricing/";
      case "cursor" -> "https://cursor.com/dashboard";
      case "copilot" -> "https://github.com/settings/billing";
      default -> "https://chatgpt.com/codex/cloud/settings/usage";
    };
  }

  private static String wrapQuota(String text) {
    StringBuilder html = new StringBuilder("<html>");
    for (String paragraph : text.split("\\n", -1)) {
      if (html.length() > 6) html.append("<br>");
      int length = 0;
      for (String word : paragraph.split(" ")) {
        if (length > 0 && length + word.length() + 1 > 30) {
          html.append("<br>");
          length = 0;
        } else if (length > 0) {
          html.append(' ');
          length++;
        }
        html.append(escape(word));
        length += word.length();
      }
    }
    return html.append("</html>").toString();
  }

  private JPanel simulationPanel() {
    JPanel box = panel();
    box.setBackground(new Color(21, 48, 58));
    box.setLayout(new BorderLayout(0, 7));
    box.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(new Color(220, 159, 92), 2),
        new EmptyBorder(10, 13, 10, 13)));
    box.setPreferredSize(new Dimension(200, 175));
    JPanel heading = new JPanel(new GridLayout(2, 1, 0, 2));
    heading.setOpaque(false);
    heading.add(label("SIMULE A ECONOMIA · CLIQUE EM RETIRAR", 14, Font.BOLD, TOKEN_OUTPUT));
    simulationNote.setForeground(MUTED);
    heading.add(simulationNote);
    box.add(heading, BorderLayout.NORTH);
    JPanel choices = new JPanel(new GridLayout(1, 4, 8, 0));
    choices.setOpaque(false);
    for (String key : List.of("skill", "agent", "spec", "hook")) {
      JToggleButton toggle = new JToggleButton();
      toggle.setUI(new javax.swing.plaf.basic.BasicToggleButtonUI() {
        @Override protected void paintText(java.awt.Graphics graphics,
            javax.swing.AbstractButton button, java.awt.Rectangle rect, String text) {
          graphics.setColor(button.getForeground());
          graphics.setFont(button.getFont());
          graphics.drawString(text, rect.x, rect.y + graphics.getFontMetrics().getAscent());
        }
      });
      toggle.setOpaque(true);
      toggle.setContentAreaFilled(true);
      toggle.setSelected(true);
      toggle.setFont(new Font("Segoe UI", Font.BOLD, 13));
      toggle.setFocusPainted(false);
      toggle.setBorder(new EmptyBorder(7, 9, 7, 9));
      toggle.addActionListener(event -> updateSimulation());
      simulationButtons.put(key, toggle);
      choices.add(toggle);
    }
    box.add(choices, BorderLayout.CENTER);
    JPanel results = new JPanel(new GridLayout(1, 3, 8, 0));
    results.setOpaque(false);
    results.setPreferredSize(new Dimension(200, 68));
    results.add(metricCard("CENÁRIO SIMULADO", simulatedTotal, TOKEN_OUTPUT, new Color(70, 49, 43)));
    results.add(metricCard("ECONOMIA ESTIMADA", simulatedSavings, new Color(167, 224, 186), new Color(28, 59, 52)));
    results.add(metricCard("ENTRADA NO CENÁRIO", simulatedInput, TOKEN_INPUT, new Color(23, 57, 76)));
    box.add(results, BorderLayout.SOUTH);
    return box;
  }

  private void updateSimulation() {
    if (updatingSimulation) return;
    Set<String> excluded = new HashSet<>();
    for (Map.Entry<String, JToggleButton> entry : simulationButtons.entrySet()) {
      String key = entry.getKey();
      JToggleButton button = entry.getValue();
      EventLogic.Source source = current == null ? null : current.inputSources().get(key);
      long estimate = source == null || source.estimatedTokens() == null
          ? 0 : Math.max(0, source.estimatedTokens());
      boolean found = source != null && source.fileCount() > 0 && estimate > 0;
      if (!found && !button.isSelected()) {
        updatingSimulation = true;
        button.setSelected(true);
        updatingSimulation = false;
      }
      button.setEnabled(found);
      if (found && !button.isSelected()) excluded.add(key);
      String name = switch (key) {
        case "skill" -> "Skills";
        case "agent" -> "Agents";
        case "spec" -> "Specs";
        default -> "Hooks";
      };
      button.setText((button.isSelected() ? "Retirar " : "Incluir ") + name
          + (found ? " · " + num(estimate) : " · —"));
      button.setBackground(!found ? new Color(51, 72, 80)
          : button.isSelected() ? new Color(37, 82, 87) : new Color(130, 70, 48));
      button.setForeground(!found ? MUTED : Color.WHITE);
      button.setToolTipText(found ? (button.isSelected() ? "Desconsiderar " : "Voltar a considerar ")
          + name + " no cenário" : "Nenhum arquivo desta origem foi identificado.");
    }
    TokenSimulation.Result result = TokenSimulation.calculate(current, excluded);
    if (!result.available()) {
      simulatedTotal.setText("—");
      simulatedInput.setText("—");
      simulatedSavings.setText("—");
      simulationNote.setText(current == null ? "Selecione uma sessão."
          : "Aguardando contagem de tokens verificável.");
    } else {
      simulatedTotal.setText("≈ " + num(result.scenario()));
      simulatedInput.setText("≈ " + num(result.input()));
      simulatedSavings.setText("≈ " + num(result.savings()));
      simulationNote.setText(excluded.isEmpty()
          ? "Clique em Retirar para montar um cenário. O total reportado permanece igual."
          : "Desconto único de estimativas locais, limitado à entrada sem cache. Não é economia comprovada.");
    }
    if (current != null) {
      for (int row = 0; row < liveSourceModel.getRowCount(); row++) {
        String key = String.valueOf(liveSourceModel.getValueAt(row, 0));
        if ("cache".equals(key)) continue;
        EventLogic.Source source = current.inputSources().get(key);
        Long estimate = source == null ? null : source.estimatedTokens();
        liveSourceModel.setValueAt(excluded.contains(key) ? "0"
            : estimate == null ? "—" : num(estimate), row, 1);
        liveSourceModel.setValueAt(excluded.contains(key) ? "Fora do cenário"
            : "Estimativa local", row, 2);
      }
    }
  }
  private void requestRefresh() {
    if (!SwingUtilities.isEventDispatchThread()) { SwingUtilities.invokeLater(this::requestRefresh); return; }
    if (sessionStarting || loading) { refreshPending = true; return; }
    loading = true;
    String id = selectedId;
    new SwingWorker<Snapshot, Void>() {
      @Override protected Snapshot doInBackground() throws Exception {
        List<Store.Run> runs = store.runs();
        String target = id.isEmpty() && !runs.isEmpty() ? runs.getFirst().id() : id;
        return new Snapshot(runs, target.isEmpty() ? null : store.detail(target),
            target.isEmpty() ? List.of() : store.quotaSnapshots(target));
      }
      @Override protected void done() {
        loading = false;
        if (sessionStarting) { refreshPending = true; return; }
        if (!id.equals(selectedId)) { refreshPending = false; requestRefresh(); return; }
        try { apply(get()); }
        catch (Exception error) {
          liveState.setText("Falha ao atualizar os dados");
          liveState.setToolTipText(error.getMessage());
        }
        if (refreshPending) { refreshPending = false; requestRefresh(); }
      }
    }.execute();
  }

  private record Snapshot(List<Store.Run> runs, Store.Detail detail, List<Store.QuotaSnapshot> quota) {}

  private void apply(Snapshot snapshot) {
    applying = true;
    runModel.clear();
    for (Store.Run run : snapshot.runs()) runModel.addElement(run);
    current = snapshot.detail();
    currentQuota = snapshot.quota();
    if (current != null) {
      selectedId = current.run().id();
      for (int i = 0; i < runModel.size(); i++) if (runModel.get(i).id().equals(selectedId)) { runList.setSelectedIndex(i); break; }
    }
    applying = false;
    updateDetail();
  }

  private void updateDetail() {
    boolean present = current != null;
    stopButton.setEnabled(present && runner.isActive(current.run().id()));
    stopButton.setVisible(present && runner.isActive(current.run().id()));
    restartButton.setEnabled(present && !runner.isActive(current.run().id()));
    deleteButton.setEnabled(present && !runner.isActive(current.run().id()));
    rawButton.setEnabled(present && Files.exists(store.rawFile(current.run().id())));
    pdfButton.setEnabled(present);
    boolean running = sessionStarting || present && "running".equals(current.run().status());
    if (!present) {
      title.setText("Nenhuma sessão selecionada"); subtitle.setText("Inicie uma sessão para acompanhar sua IA.");
      clear(liveEventModel); clear(liveSourceModel);
      liveState.setText("Aguardando sessão");
      promptPreview.setText("Pedido: —");
      simulationSessionId = "";
      updateSimulation();
      liveCount.setText("0 operações · 0 arquivos");
      liveTotal.setText("—");
      liveTotalState.setText("AGUARDANDO");
      liveTotalEquation.setText("");
      quotaTitle.setText("ASSINATURA · SELECIONE UMA SESSÃO");
      quotaHeadline.setText("Cota: —");
      quotaDetail.setText("");
      quotaNote.setText("");
      quotaSource.setEnabled(false);
      executionPhase.setText(sessionStarting ? "INICIANDO" : "AGUARDANDO");
      executionHeadline.setText(sessionStarting ? "Conectando à CLI" : "Selecione uma sessão");
      executionDetail.setText(sessionStarting ? "Iniciando o processo da IA na pasta do projeto..." : "Inicie uma sessão para acompanhar as operações.");
      executionMeta.setText("00:00:00  ·  0 eventos  ·  0 arquivos");
      executionHero.setVisible(sessionStarting);
      executionPulse.setVisible(sessionStarting);
      usageChart.setData(WorkbenchMetrics.from(null));
      artifactChart.setData(WorkbenchMetrics.from(null));
      for (JLabel row : recentEvents) row.setText("");
      return;
    }
    Store.Run run = current.run();
    if (!run.id().equals(simulationSessionId)) {
      simulationSessionId = run.id();
      updatingSimulation = true;
      for (JToggleButton button : simulationButtons.values()) button.setSelected(true);
      updatingSimulation = false;
    }
    title.setText("Sessão " + run.id().substring(0, 8).toUpperCase() + "  ·  " + statusName(run.status()));
    subtitle.setText(Provider.name(run.provider()) + "  ·  Projeto: " + Path.of(run.project()).getFileName()
        + (run.model() == null || run.model().isBlank() ? "" : "  ·  " + run.model()));
    subtitle.setToolTipText(run.project() + " · " + run.sandbox());
    promptPreview.setText("Pedido: " + clipped(run.prompt().replace('\n', ' '), 130));
    boolean reported = hasReportedUsage(run);
    TokenAccounting.Assessment assessment = TokenAccounting.assess(run, reported, current.usageAuditable());
    liveTotal.setText(assessment.total());
    liveTotalState.setText(assessment.status());
    liveTotalEquation.setText(assessment.equation());
    QuotaPresentation.View quotaView = QuotaPresentation.describe(run.provider(), currentQuota);
    quotaTitle.setText("ASSINATURA · " + Provider.name(run.provider()));
    quotaHeadline.setText(wrapQuota(quotaView.headline()));
    quotaDetail.setText(wrapQuota(quotaView.detail()));
    quotaNote.setText(wrapQuota(quotaView.note()));
    quotaSource.setEnabled(true);
    Color countColor = assessment.finalNumber() ? ACCENT : new Color(239, 192, 120);
    liveTotalState.setForeground(countColor);
    liveState.setText("running".equals(run.status()) ? "● IA em execução" : "Sessão " + statusName(run.status()));
    liveState.setForeground("running".equals(run.status()) ? ACCENT : INK);
    ExecutionNarrative.State narrative = ExecutionNarrative.describe(run, current.events(),
        current.artifacts().size(), runner.isActive(run.id()));
    executionPhase.setText(narrative.phase());
    executionHeadline.setText(clipped(narrative.headline(), 95));
    executionDetail.setText(clipped(narrative.detail(), 145));
    executionDetail.setToolTipText(narrative.detail());
    executionMeta.setText(narrative.meta());
    executionPulse.setVisible(narrative.working());
    executionHero.setVisible(running || "failed".equals(run.status()) || "interrupted".equals(run.status()));
    executionHero.setBackground(narrative.working() ? new Color(27, 68, 76)
        : "completed".equals(run.status()) ? new Color(31, 68, 57) : new Color(78, 49, 48));
    liveCount.setText(current.events().size() + " operações recentes · " + current.artifacts().size() + " arquivos identificados");
    WorkbenchMetrics.Snapshot metrics = WorkbenchMetrics.from(current);
    usageChart.setData(metrics);
    artifactChart.setData(metrics);
    for (int i = 0; i < recentEvents.length; i++) {
      int index = current.events().size() - 1 - i;
      if (index < 0) recentEvents[i].setText(i == 0 ? "Aguardando eventos da CLI…" : "");
      else {
        Store.Event event = current.events().get(index);
        recentEvents[i].setText(date(event.at()) + "   " + clipped(event.title(), 38)
            + (event.detail().isBlank() ? "" : "  ·  " + clipped(event.detail(), 110)));
        recentEvents[i].setToolTipText(event.detail());
      }
    }
    clear(liveEventModel);
    for (Store.Event event : current.events()) {
      EventLogic.Usage usage = event.usage();
      liveEventModel.addRow(new Object[]{date(event.at()), event.title(), clipped(event.detail(), 180),
          usage == null ? "—" : num(usage.input()), usage == null ? "—" : num(usage.cached()),
          usage == null ? "—" : num(usage.output())});
    }
    if ("running".equals(run.status()) && liveEventTable.getRowCount() > 0) {
      int last = liveEventTable.getRowCount() - 1;
      liveEventTable.scrollRectToVisible(liveEventTable.getCellRect(last, 0, true));
    }
    clear(liveSourceModel);
    for (Map.Entry<String, EventLogic.Source> entry : current.inputSources().entrySet()) {
      Long estimate = entry.getValue().estimatedTokens();
      liveSourceModel.addRow(new Object[]{entry.getKey(), estimate == null ? "—" : num(estimate), "Estimado"});
    }
    if (current.usageAuditable() && run.cachedTokens() > 0)
      liveSourceModel.addRow(new Object[]{"cache", num(run.cachedTokens()), "Reportado"});
    int sourceHeight = liveSourceTable.getTableHeader().getPreferredSize().height
        + liveSourceModel.getRowCount() * liveSourceTable.getRowHeight() + 3;
    liveSourceScroll.setPreferredSize(new Dimension(200, sourceHeight));
    liveSourceScroll.revalidate();
    updateSimulation();
  }

  private void registerSourceDetails(JTable sourceTable) {
    sourceTable.addMouseListener(new MouseAdapter() {
      @Override public void mouseClicked(MouseEvent event) {
        if (current == null) return;
        int row = sourceTable.rowAtPoint(event.getPoint());
        if (row < 0) return;
        String source = sourceTable.getValueAt(row, 0).toString();
        if (event.getClickCount() < 1) return;
        if ("cache".equals(source)) showCacheDetails(null);
        else showSourceDetails(source);
      }
    });
  }

  private void showDataTrace() {
    Store.Detail snapshot = current;
    if (snapshot == null) return;
    DetailsDialog.open(this, "Dados do projeto e envio remoto", "Trilha de dados",
        "Sessão " + snapshot.run().id().substring(0, 8).toUpperCase()
            + " · evidência local; corpo HTTPS indisponível",
        () -> {
          try {
            return DataTrace.describe(DataTrace.from(snapshot.run(),
                store.traceEvents(snapshot.run().id()), snapshot.artifacts()));
          } catch (Exception error) {
            return "Não foi possível consultar a trilha de dados: " + error.getMessage();
          }
        });
  }
  private void showCacheDetails(Integer sequence) {
    if (current != null) CacheInspector.open(this, store, current, sequence);
  }

  private void showSourceDetails(String source) {
    Store.Detail snapshot = current;
    if (snapshot == null || ("cache".equals(source) && snapshot.run().cachedTokens() <= 0)) return;
    String name = switch (source) {
      case "prompt" -> "Prompt simples";
      case "spec" -> "Specs";
      case "skill" -> "Skills";
      case "agent" -> "Agents";
      case "hook" -> "Hooks";
      case "cache" -> "Cache reportado";
      default -> source;
    };
    DetailsDialog.open(this, "Detalhes da origem: " + name, name,
        "Sessão " + snapshot.run().id().substring(0, 8).toUpperCase()
            + " · dados observados nesta execução",
        () -> SourceDetails.describe(store, snapshot, source));
  }
  private void showArtifactCategory(String category) {
    Store.Detail snapshot = current;
    if (snapshot == null) return;
    DetailsDialog.open(this, "Artefatos: " + category, category,
        "Sessão " + snapshot.run().id().substring(0, 8).toUpperCase()
            + " · arquivos identificados localmente",
        () -> SourceDetails.describeCategory(snapshot, category));
  }
  private boolean hasReportedUsage(Store.Run run) {
    return run.totalTokens() > 0 || run.inputTokens() + run.outputTokens() > 0 || current != null
        && current.events().stream().anyMatch(event -> event.usage() != null);
  }

  private void showAbout() {
    Path root = Path.of("").toAbsolutePath().normalize();
    if (!Files.isRegularFile(root.resolve("pom.xml"))) root = store.dataDirectory().getParent();
    AboutDialog.open(this, store, current, root);
  }

  private void showUsageHelp() {
    UsageGuide.open(this, store, current);
  }

  private void newSession() {
    JDialog dialog = new JDialog(this, "Nova sessão de IA", true);
    Store.Run sourceRun = runList.getSelectedValue();
    JComboBox<String> provider = new JComboBox<>(Provider.IDS.toArray(String[]::new));
    provider.setRenderer(new DefaultListCellRenderer() {
      @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
        super.getListCellRendererComponent(list, Provider.name(String.valueOf(value)), index, selected, focus);
        return this;
      }
    });
    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    JPanel form = ModalStyle.panel();
    form.setLayout(new BorderLayout(0, 18));
    form.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(ModalStyle.BORDER, 3), new EmptyBorder(20, 24, 18, 24)));

    JPanel header = ModalStyle.panel();
    header.setBackground(ModalStyle.HEADER);
    header.setBorder(new EmptyBorder(12, 16, 13, 16));
    header.setLayout(new GridLayout(3, 1, 0, 4));
    header.add(label("NOVA SESSÃO  ·  JANELA DE CONFIGURAÇÃO", 11, Font.BOLD, new Color(255, 221, 180)));
    header.add(label("Escolha a IA e a pasta do projeto.", 19, Font.BOLD, Color.WHITE));
    header.add(label("O monitor acompanha uma nova execução pela CLI oficial.", 12, Font.PLAIN, Color.WHITE));
    form.add(header, BorderLayout.NORTH);

    JTextArea connection = new JTextArea();
    connection.setEditable(false); connection.setLineWrap(true); connection.setWrapStyleWord(true);
    connection.setBackground(ModalStyle.PAPER); connection.setForeground(ModalStyle.INK);
    connection.setFont(new Font("Segoe UI", Font.PLAIN, 12));
    connection.setBorder(new EmptyBorder(12, 14, 12, 14));
    connection.setFocusable(false);
    JScrollPane connectionScroll = new JScrollPane(connection);
    connectionScroll.setPreferredSize(new Dimension(600, 165));
    connectionScroll.setMinimumSize(new Dimension(300, 165));
    connectionScroll.setBorder(BorderFactory.createLineBorder(ModalStyle.BORDER));
    JPanel connectionBlock = ModalStyle.panel();
    connectionBlock.setLayout(new BorderLayout(0, 8));
    connectionBlock.add(label("LOGIN, ACESSO E MEDIÇÃO", 11, Font.BOLD, ModalStyle.ACCENT), BorderLayout.NORTH);
    connectionBlock.add(connectionScroll, BorderLayout.CENTER);
    provider.addActionListener(event -> {
      connection.setText(Provider.explanation((String) provider.getSelectedItem()));
      connection.setCaretPosition(0);
    });
    connection.setText(Provider.explanation((String) provider.getSelectedItem()));
    connection.setCaretPosition(0);
    JTextField project = new JTextField(current == null ? System.getProperty("user.dir") : current.run().project());
    styleInput(project);
    JButton browse = ModalStyle.button("Escolher pasta", false);
    browse.addActionListener(event -> {
      JFileChooser chooser = new JFileChooser(project.getText());
      chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
      if (chooser.showOpenDialog(dialog) == JFileChooser.APPROVE_OPTION)
        project.setText(chooser.getSelectedFile().getAbsolutePath());
    });
    JPanel pathRow = ModalStyle.panel();
    pathRow.setLayout(new BorderLayout(9, 0));
    pathRow.add(project, BorderLayout.CENTER);
    pathRow.add(browse, BorderLayout.EAST);

    JTextArea request = new JTextArea();
    request.setLineWrap(true); request.setWrapStyleWord(true);
    request.setFont(new Font("Segoe UI", Font.PLAIN, 14));
    request.setBackground(ModalStyle.PAPER); request.setForeground(ModalStyle.INK); request.setCaretColor(ModalStyle.ACCENT);
    request.setBorder(new EmptyBorder(12, 14, 12, 14));
    JScrollPane requestScroll = new JScrollPane(request);
    requestScroll.setBorder(BorderFactory.createLineBorder(ModalStyle.BORDER));
    requestScroll.setPreferredSize(new Dimension(600, 135));
    requestScroll.setMinimumSize(new Dimension(300, 105));

    JTextField model = new JTextField();
    styleInput(model);
    JRadioButton writePermission = permission("Editar projeto", true);
    JRadioButton readPermission = permission("Somente leitura", false);
    ButtonGroup permissionGroup = new ButtonGroup();
    permissionGroup.add(writePermission); permissionGroup.add(readPermission);
    JButton reuse = ModalStyle.button("Reutilizar sessão selecionada", false);
    reuse.setEnabled(sourceRun != null);
    reuse.setToolTipText(sourceRun == null
        ? "Selecione uma sessão na lista antes de abrir Nova sessão."
        : "Copia o pedido e a configuração para revisão; cria uma nova execução somente ao confirmar.");
    reuse.addActionListener(event -> {
      if (sourceRun == null) return;
      provider.setSelectedItem(sourceRun.provider());
      project.setText(sourceRun.project());
      request.setText(sourceRun.prompt());
      request.setCaretPosition(0);
      model.setText(sourceRun.model() == null ? "" : sourceRun.model());
      if ("read-only".equals(sourceRun.sandbox())) readPermission.setSelected(true);
      else writePermission.setSelected(true);
    });
    JPanel permissionOptions = ModalStyle.panel();
    permissionOptions.setLayout(new GridLayout(1, 2, 8, 0));
    permissionOptions.add(writePermission); permissionOptions.add(readPermission);
    JPanel options = ModalStyle.panel();
    options.setLayout(new GridLayout(1, 2, 15, 0));
    JPanel modelBlock = ModalStyle.panel();
    modelBlock.setLayout(new BorderLayout(0, 7));
    modelBlock.add(label("MODELO (OPCIONAL)", 11, Font.BOLD, ModalStyle.MUTED), BorderLayout.NORTH);
    modelBlock.add(model, BorderLayout.CENTER);
    JPanel permissionBlock = ModalStyle.panel();
    permissionBlock.setLayout(new BorderLayout(0, 7));
    permissionBlock.add(label("PERMISSÃO", 11, Font.BOLD, ModalStyle.MUTED), BorderLayout.NORTH);
    permissionBlock.add(permissionOptions, BorderLayout.CENTER);
    options.add(modelBlock); options.add(permissionBlock);

    JPanel fields = ModalStyle.panel();
    fields.setLayout(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();
    c.gridx = 0; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.anchor = GridBagConstraints.WEST;
    c.gridy = 0; c.insets = new Insets(0, 0, 7, 0);
    fields.add(label("IA A MONITORAR", 11, Font.BOLD, ModalStyle.MUTED), c);
    c.gridy = 1; c.insets = new Insets(0, 0, 7, 0); fields.add(provider, c);
    c.gridy = 2; c.insets = new Insets(0, 0, 12, 0); fields.add(connectionBlock, c);
    c.gridy = 3; c.insets = new Insets(0, 0, 7, 0);
    fields.add(label("PASTA DO PROJETO", 11, Font.BOLD, ModalStyle.MUTED), c);
    c.gridy = 4; c.insets = new Insets(0, 0, 16, 0);
    fields.add(pathRow, c);
    c.gridy = 5; c.insets = new Insets(0, 0, 7, 0);
    JPanel requestHeading = ModalStyle.panel();
    requestHeading.setLayout(new BorderLayout(10, 0));
    requestHeading.add(label("PEDIDO PARA A IA", 11, Font.BOLD, ModalStyle.MUTED), BorderLayout.WEST);
    requestHeading.add(reuse, BorderLayout.EAST);
    fields.add(requestHeading, c);
    c.gridy = 6; c.weighty = 1; c.fill = GridBagConstraints.BOTH; c.insets = new Insets(0, 0, 19, 0);
    fields.add(requestScroll, c);
    c.gridy = 7; c.weighty = 0; c.fill = GridBagConstraints.HORIZONTAL; c.insets = new Insets(0, 0, 0, 0);
    fields.add(label("Para comparar execuções, use cópias iguais do projeto: a primeira execução modifica a pasta.",
        12, Font.PLAIN, ModalStyle.MUTED), c);
    c.gridy = 8; c.insets = new Insets(8, 0, 0, 0);
    fields.add(options, c);
    form.add(fields, BorderLayout.CENTER);

    JPanel footer = ModalStyle.panel();
    footer.setLayout(new FlowLayout(FlowLayout.RIGHT, 10, 0));
    JButton cancel = ModalStyle.button("Cancelar", false);
    JButton execute = ModalStyle.button("Executar e acompanhar", true);
    cancel.addActionListener(event -> dialog.dispose());
    execute.addActionListener(event -> {
      Path projectPath;
      try { projectPath = Path.of(project.getText()); }
      catch (RuntimeException error) {
        JOptionPane.showMessageDialog(dialog, "Escolha uma pasta de projeto válida.", "Pasta inválida", JOptionPane.WARNING_MESSAGE);
        return;
      }
      if (!Files.isDirectory(projectPath)) {
        JOptionPane.showMessageDialog(dialog, "Escolha uma pasta de projeto existente.", "Pasta inválida", JOptionPane.WARNING_MESSAGE);
        return;
      }
      if (request.getText().isBlank()) {
        JOptionPane.showMessageDialog(dialog, "Escreva o pedido para a IA.", "Pedido vazio", JOptionPane.WARNING_MESSAGE);
        request.requestFocusInWindow();
        return;
      }
      String providerId = (String) provider.getSelectedItem();
      String requestText = request.getText();
      String modelText = model.getText();
      String permission = writePermission.isSelected() ? "workspace-write" : "read-only";
      dialog.dispose();
      getGlassPane().setVisible(false);
      sessionStarting = true;
      workTabs.setSelectedIndex(0);
      selectedId = "";
      applying = true;
      runList.clearSelection();
      applying = false;
      current = null;
      updateDetail();
      title.setText("Iniciando nova sessão");
      subtitle.setText(projectPath.toString());
      liveState.setText("Preparando execução da IA...");
      new SwingWorker<Store.Run, Void>() {
        @Override protected Store.Run doInBackground() throws Exception {
          return runner.start(projectPath, requestText, modelText, permission, providerId);
        }
        @Override protected void done() {
          sessionStarting = false;
          refreshPending = false;
          try {
            selectedId = get().id();
            requestRefresh();
          } catch (Exception error) {
            JOptionPane.showMessageDialog(Dashboard.this, cause(error), "Não foi possível iniciar", JOptionPane.ERROR_MESSAGE);
            requestRefresh();
          }
        }
      }.execute();
    });
    footer.add(cancel); footer.add(execute);
    form.add(footer, BorderLayout.SOUTH);
    dialog.setContentPane(form);
    dialog.setSize(860, 820);
    dialog.setMinimumSize(new Dimension(700, 690));
    dialog.setLocationRelativeTo(this);
    ModalStyle.show(this, dialog);
  }

  private void restartSession() {
    if (current == null || runner.isActive(current.run().id())) return;
    String id = current.run().id();
    if (JOptionPane.showConfirmDialog(this,
        "Apagar eventos, artefatos, amostras e tokens desta sessão e executar novamente o mesmo pedido?",
        "Zerar e executar", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
    restartButton.setEnabled(false);
    workTabs.setSelectedIndex(0);
    new SwingWorker<Store.Run, Void>() {
      @Override protected Store.Run doInBackground() throws Exception { return runner.restart(id); }
      @Override protected void done() {
        try { selectedId = get().id(); requestRefresh(); }
        catch (Exception error) {
          JOptionPane.showMessageDialog(Dashboard.this, cause(error), "Não foi possível reiniciar", JOptionPane.ERROR_MESSAGE);
          requestRefresh();
        }
      }
    }.execute();
  }

  private void deleteSession() {
    if (current == null || runner.isActive(current.run().id())) return;
    String id = current.run().id();
    if (JOptionPane.showConfirmDialog(this,
        "Excluir permanentemente esta sessão, seus eventos, artefatos, amostras e arquivo JSONL?",
        "Excluir sessão", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
    deleteButton.setEnabled(false);
    new SwingWorker<Void, Void>() {
      @Override protected Void doInBackground() throws Exception { store.delete(id); return null; }
      @Override protected void done() {
        try { get(); selectedId = ""; current = null; requestRefresh(); }
        catch (Exception error) {
          JOptionPane.showMessageDialog(Dashboard.this, cause(error), "Não foi possível excluir", JOptionPane.ERROR_MESSAGE);
          requestRefresh();
        }
      }
    }.execute();
  }

  private void exportRaw() {
    if (current == null) return;
    Path source = store.rawFile(current.run().id());
    Path target = chooseSave("Eventos JSONL", current.run().id() + ".jsonl");
    if (target == null) return;
    try { Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING); }
    catch (IOException error) { JOptionPane.showMessageDialog(this, error.getMessage(), "Exportação falhou", JOptionPane.ERROR_MESSAGE); }
  }

  private void exportPdf() {
    if (current == null) return;
    Path target = chooseSave("Relatório PDF", "iamonitor-" + current.run().id().substring(0, 8) + ".pdf");
    if (target == null) return;
    String id = current.run().id();
    new SwingWorker<Void, Void>() {
      @Override protected Void doInBackground() throws Exception {
        Store.Detail snapshot = store.reportDetail(id);
        if (snapshot == null) throw new IOException("A sessão não está mais disponível.");
        ReportPdf.write(snapshot, DataTrace.from(snapshot.run(),
            store.traceEvents(id), snapshot.artifacts()), target);
        return null;
      }
      @Override protected void done() {
        try { get(); JOptionPane.showMessageDialog(Dashboard.this, "Relatório salvo em " + target); }
        catch (Exception error) { JOptionPane.showMessageDialog(Dashboard.this, cause(error), "Exportação falhou", JOptionPane.ERROR_MESSAGE); }
      }
    }.execute();
  }

  private Path chooseSave(String title, String suggested) {
    JFileChooser chooser = new JFileChooser();
    chooser.setDialogTitle(title);
    chooser.setSelectedFile(new java.io.File(suggested));
    if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return null;
    Path target = chooser.getSelectedFile().toPath();
    if (Files.exists(target) && JOptionPane.showConfirmDialog(this, "Substituir o arquivo existente?",
        title, JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return null;
    return target;
  }

  private static String cause(Exception error) {
    Throwable value = error;
    while (value.getCause() != null) value = value.getCause();
    return value.getMessage() == null ? value.toString() : value.getMessage();
  }

  private static JPanel panel() { JPanel panel = new JPanel(); panel.setBackground(BG); return panel; }
  private static JButton button(String text) {
    JButton button = new JButton(text);
    button.setUI(new javax.swing.plaf.basic.BasicButtonUI());
    button.setFont(new Font("Segoe UI", Font.BOLD, 11));
    button.setBackground(new Color(28, 49, 64));
    button.setForeground(INK);
    button.setFocusPainted(false);
    button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(52, 84, 99)), new EmptyBorder(8, 12, 8, 12)));
    return button;
  }
  private static JLabel label(String text, int size, int style, Color color) {
    JLabel label = new JLabel(text); label.setFont(new Font("Segoe UI", style, size)); label.setForeground(color); label.setAlignmentX(LEFT_ALIGNMENT); label.setHorizontalAlignment(JLabel.LEFT); label.setMaximumSize(new Dimension(Integer.MAX_VALUE, label.getPreferredSize().height)); return label;
  }
  private static JRadioButton permission(String text, boolean selected) {
    JRadioButton radio = new JRadioButton(text, selected);
    radio.setFont(new Font("Segoe UI", Font.PLAIN, 12));
    radio.setBackground(ModalStyle.PAPER); radio.setForeground(ModalStyle.INK);
    radio.setFocusPainted(false);
    radio.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(ModalStyle.BORDER), new EmptyBorder(7, 8, 7, 8)));
    return radio;
  }
  private static void styleInput(JTextField field) {
    field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
    field.setBackground(ModalStyle.PAPER); field.setForeground(ModalStyle.INK); field.setCaretColor(ModalStyle.ACCENT);
    field.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(ModalStyle.BORDER), new EmptyBorder(9, 11, 9, 11)));
  }
  private static JLabel value() {
    JLabel value = label("—", 21, Font.BOLD, ACCENT);
    value.setHorizontalAlignment(JLabel.CENTER);
    return value;
  }
  private static JPanel totalPanel(JLabel totalValue, JLabel state, JLabel equation) {
    JPanel hero = new JPanel(new BorderLayout(0, 4));
    hero.setBackground(new Color(28, 68, 79));
    hero.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(ACCENT, 2),
        new EmptyBorder(10, 15, 9, 15)));
    hero.setPreferredSize(new Dimension(200, 110));
    hero.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
    hero.setAlignmentX(LEFT_ALIGNMENT);
    JPanel heading = new JPanel(new BorderLayout());
    heading.setOpaque(false);
    heading.add(label("TOKENS DA CLI · NÃO É COBRANÇA", 12, Font.BOLD, INK), BorderLayout.WEST);
    totalValue.setFont(new Font("Segoe UI", Font.BOLD, 34));
    totalValue.setForeground(Color.WHITE);
    heading.add(totalValue, BorderLayout.EAST);
    hero.add(heading, BorderLayout.NORTH);
    JPanel explanation = new JPanel();
    explanation.setOpaque(false);
    explanation.setLayout(new GridLayout(2, 1, 0, 3));
    state.setForeground(ACCENT);
    explanation.add(state);
    explanation.add(equation);
    hero.setToolTipText("Entrada = modelo recebeu; saída = modelo gerou. Cache integra a entrada.");

    hero.add(explanation, BorderLayout.CENTER);
    return hero;
  }
  private static JPanel metricCard(String caption, JLabel value, Color accent, Color background) {
    JPanel metric = new JPanel(new BorderLayout(0, 7));
    metric.setBackground(background);
    metric.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(accent, 2),
        new EmptyBorder(10, 12, 10, 12)));
    JLabel heading = label(caption, 10, Font.BOLD, accent);
    heading.setHorizontalAlignment(JLabel.CENTER);
    value.setForeground(accent);
    metric.add(heading, BorderLayout.NORTH);
    metric.add(value, BorderLayout.CENTER);
    return metric;
  }

  private static DefaultTableModel model(String... columns) {
    return new DefaultTableModel(columns, 0) { @Override public boolean isCellEditable(int row, int column) { return false; } };
  }
  private static JTable table(DefaultTableModel model) {
    JTable table = new JTable(model) {
      @Override public String getToolTipText(MouseEvent event) {
        int row = rowAtPoint(event.getPoint()), column = columnAtPoint(event.getPoint());
        if (row < 0 || column < 0) return null;
        Object value = getValueAt(row, column);
        return value == null ? null : value.toString();
      }
    };
    table.setRowHeight(32); table.setFillsViewportHeight(true);
    table.setBackground(PANEL); table.setForeground(INK); table.setGridColor(new Color(42, 62, 79));
    table.setSelectionBackground(new Color(37, 83, 91));
    table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
    for (int column = 0; column < table.getColumnCount(); column++) {
      DefaultTableCellRenderer cell = new DefaultTableCellRenderer() {
        @Override public Component getTableCellRendererComponent(JTable owner, Object value,
            boolean selected, boolean focused, int row, int column) {
          super.getTableCellRendererComponent(owner, value, selected, focused, row, column);
          setBorder(new EmptyBorder(0, 12, 0, 12));
          return this;
        }
      };
      cell.setBackground(PANEL); cell.setForeground(INK);
      table.getColumnModel().getColumn(column).setCellRenderer(cell);
      DefaultTableCellRenderer header = new DefaultTableCellRenderer() {
        @Override public Component getTableCellRendererComponent(JTable owner, Object value,
            boolean selected, boolean focused, int row, int column) {
          super.getTableCellRendererComponent(owner, value, selected, focused, row, column);
          setBackground(new Color(28, 47, 63)); setForeground(INK);
          setBorder(new EmptyBorder(0, 12, 0, 12));
          return this;
        }
      };
      header.setOpaque(true);
      header.setFont(new Font("Segoe UI", Font.BOLD, 11));
      table.getColumnModel().getColumn(column).setHeaderRenderer(header);
    }
    table.getTableHeader().setBackground(new Color(28, 47, 63));
    table.getTableHeader().setForeground(INK);
    table.getTableHeader().setPreferredSize(new Dimension(0, 36));
    table.setAutoCreateRowSorter(true);
    return table;
  }
  private static void clear(DefaultTableModel model) { model.setRowCount(0); }
  private static String statusName(String status) {
    return switch (status) {
      case "running" -> "Em execução";
      case "completed" -> "Concluída";
      case "failed" -> "Falhou";
      case "stopped" -> "Interrompida";
      default -> status;
    };
  }
  private static String num(long value) { return NUM.format(value); }
  private static String date(String value) {
    try { return DATE.format(Instant.parse(value)); } catch (RuntimeException ignored) { return value; }
  }
  private static String clipped(String value, int max) {
    return value.length() <= max ? value : value.substring(0, max - 1) + "…";
  }
  private static String escape(String value) {
    return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
  }

}

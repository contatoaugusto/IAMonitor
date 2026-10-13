package br.iamonitor.desktop;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

final class AboutDialog {
  private static final Color PAPER = ModalStyle.PAPER;
  private static final Color MUTED = ModalStyle.MUTED;
  private static final Color INK = ModalStyle.INK;
  private static final Color BLUE = new Color(40, 91, 111);
  private static final NumberFormat NUM = NumberFormat.getIntegerInstance(Locale.forLanguageTag("pt-BR"));

  private AboutDialog() {}

  static void open(JFrame owner, Store.Detail selected) {
    open(owner, selected, Path.of("").toAbsolutePath().normalize(),
        Path.of(System.getProperty("user.home"), ".codex", "sessions"));
  }

  static void open(JFrame owner, Store.Detail selected, Path projectRoot) {
    open(owner, selected, projectRoot, Path.of(System.getProperty("user.home"), ".codex", "sessions"));
  }

  static void open(JFrame owner, Store.Detail selected, Path projectRoot, Path sessionsRoot) {
    open(owner, null, selected, projectRoot, sessionsRoot);
  }

  static void open(JFrame owner, Store store, Store.Detail selected, Path projectRoot) {
    open(owner, store, selected, projectRoot,
        Path.of(System.getProperty("user.home"), ".codex", "sessions"));
  }

  private static void open(JFrame owner, Store store, Store.Detail selected,
      Path projectRoot, Path sessionsRoot) {
    JDialog dialog = new JDialog(owner, "Sobre o IAMonitor", true);
    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    JPanel root = ModalStyle.panel();
    root.setLayout(new BorderLayout(0, 0));
    JPanel header = new JPanel(new GridLayout(3, 1, 0, 4));
    header.setBackground(ModalStyle.HEADER);
    header.setBorder(new EmptyBorder(17, 25, 17, 25));
    header.add(label("AJUDA  /  SOBRE", 11, Font.BOLD, new Color(255, 216, 175)));
    header.add(label("Como o IAMonitor foi construído", 24, Font.BOLD, Color.WHITE));
    header.add(label(Branding.load().name() + " · Arquitetura, tecnologias, decisões e contagens da aplicação.",
        13, Font.PLAIN, Color.WHITE));
    root.add(header, BorderLayout.NORTH);

    JPanel pages = ModalStyle.panel();
    CardLayout cards = new CardLayout();
    pages.setLayout(cards);
    pages.add(scroll(overview()), "Visão geral");
    pages.add(scroll(architecture()), "Arquitetura");
    pages.add(scroll(decisions()), "Decisões");
    pages.add(scroll(aiAndTokens(dialog, store, selected, projectRoot, sessionsRoot)), "IA e tokens");

    JPanel nav = ModalStyle.panel();
    nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
    nav.setBorder(new EmptyBorder(20, 16, 16, 12));
    nav.setPreferredSize(new Dimension(215, 0));
    List<JButton> choices = new ArrayList<>();
    for (String name : List.of("Visão geral", "Arquitetura", "Decisões", "IA e tokens")) {
      JButton button = new JButton(name);
      button.setUI(new javax.swing.plaf.basic.BasicButtonUI());
      button.setOpaque(true);
      button.setFont(new Font("Segoe UI", Font.BOLD, 13));
      button.setFocusPainted(false);
      button.setHorizontalAlignment(JButton.LEFT);
      button.setBorder(new EmptyBorder(12, 14, 12, 14));
      button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
      button.setAlignmentX(JPanel.LEFT_ALIGNMENT);
      choices.add(button);
      button.addActionListener(event -> {
        cards.show(pages, name);
        for (java.awt.Component page : pages.getComponents()) if (page instanceof JScrollPane scroll && page.isVisible()) {
          scroll.getViewport().setViewPosition(new java.awt.Point(0, 0));
          SwingUtilities.invokeLater(() -> scroll.getVerticalScrollBar().setValue(0));
        }
        for (JButton choice : choices) {
          boolean active = choice == button;
          choice.setBackground(active ? ModalStyle.HEADER : PAPER);
          choice.setForeground(active ? Color.WHITE : INK);
        }
      });
      nav.add(button);
      nav.add(Box.createVerticalStrut(9));
    }
    choices.getFirst().doClick();
    JPanel middle = ModalStyle.panel();
    middle.setLayout(new BorderLayout());
    middle.add(nav, BorderLayout.WEST);
    middle.add(pages, BorderLayout.CENTER);
    root.add(middle, BorderLayout.CENTER);

    JPanel footer = ModalStyle.panel();
    footer.setLayout(new FlowLayout(FlowLayout.RIGHT, 12, 8));
    footer.setBorder(new EmptyBorder(0, 16, 6, 16));
    JButton close = ModalStyle.button("Fechar", true);
    close.addActionListener(event -> dialog.dispose());
    footer.add(close);
    root.add(footer, BorderLayout.SOUTH);
    dialog.setContentPane(root);
    dialog.setSize(1120, 780);
    dialog.setMinimumSize(new Dimension(940, 650));
    dialog.setLocationRelativeTo(owner);
    ModalStyle.show(owner, dialog);
  }

  private static JPanel overview() {
    JPanel page = page("O aplicativo em uma frase",
        "Uma aplicação Java desktop que inicia uma nova tarefa em uma CLI de IA, acompanha seus eventos e apresenta os tokens que essa CLI efetivamente informa.");
    add(page, card("Para que foi criado",
        "O objetivo é acompanhar tarefas de IA em projetos, mostrar operações e arquivos identificados e distinguir contagens reportadas de estimativas locais.", BLUE));
    add(page, card("O que acontece ao executar",
        "Você escolhe IA, pasta, pedido e permissão. O IAMonitor abre a CLI oficial nessa pasta com o login que a própria CLI já possui. Ele acompanha a saída estruturada da nova execução, registra eventos e exibe o resultado.", ModalStyle.ACCENT));
    add(page, card("O que o número principal significa",
        "Total reportado pela CLI para a tarefa selecionada. Entrada são tokens recebidos pelo modelo; saída são tokens gerados pelo modelo. Cache já integra a entrada. O aplicativo não mede saldo de assinatura nem bytes da rede.", BLUE));
    add(page, card("Ao vivo e depois da conclusão",
        "Durante o trabalho, a tela privilegia a última operação observada e o histórico de eventos. Quando a CLI termina, o painel destaca o total, as origens identificadas, a simulação de economia e os relatórios.", ModalStyle.ACCENT));
    return page;
  }

  private static JPanel architecture() {
    JPanel page = page("O caminho do pedido",
        "A comunicação com o modelo remoto é feita pela CLI escolhida. O IAMonitor observa o processo local e seus eventos; ele não intercepta a rede.");
    add(page, flow(new String[][]{
        {"1 · TELA", "Swing recebe o pedido, a pasta e a IA."},
        {"2 · EXECUÇÃO", "CodexRunner inicia a CLI; Provider monta o comando."},
        {"3 · IA", "A CLI usa seu login e conversa com o serviço remoto."},
        {"4 · EVENTOS", "A CLI devolve eventos e, quando disponível, uso de tokens."}
    }));
    add(page, flow(new String[][]{
        {"5 · INTERPRETAÇÃO", "EventLogic resume eventos e identifica arquivos."},
        {"6 · REGISTRO", "Store grava SQLite, JSONL e cópias locais dos arquivos."},
        {"7 · CONTAGEM", "TokenAccounting confere o total; simulação fica separada."},
        {"8 · APRESENTAÇÃO", "Dashboard exibe os dados; PDFBox gera relatórios."}
    }));
    add(page, card("Tecnologias e versões do projeto",
        "Java 21 e Swing (interface); Maven (build); SQLite JDBC 3.53.4.0 (persistência); Jackson 2.21.7 (eventos JSON); JTokkit 1.1.0 (estimativas locais de texto); PDFBox 3.0.8 (PDF); JUnit Jupiter 5.12.2 (testes). Maven Shade reúne as dependências no JAR e jpackage cria o aplicativo Windows com runtime Java.", BLUE));
    add(page, card("CLIs integradas",
        "OpenAI Codex, Claude Code, Gemini CLI, Cursor Agent e GitHub Copilot CLI. A disponibilidade de tokens depende do fluxo estruturado de cada ferramenta. Cursor e Copilot podem mostrar operações sem informar uma contagem completa.", ModalStyle.ACCENT));
    return page;
  }

  private static JPanel decisions() {
    JPanel page = page("Decisões centrais",
        "Estas são as decisões que moldam o comportamento atual do código e ajudam a explicar os limites da medição.");
    String[][] items = {
        {"01 · Aplicativo desktop", "A versão atual usa Java Swing na máquina do usuário, com dados locais em vez de um servidor web."},
        {"02 · CLI oficial e login existente", "Cada sessão inicia uma nova execução da CLI escolhida. O IAMonitor não pede senha, não copia credenciais e não se conecta a uma sessão já aberta."},
        {"03 · Eventos como fonte da contagem", "O total principal vem do uso emitido pela CLI. Se a ferramenta não informa tokens, a tela mostra indisponível; valores locais não substituem esse total."},
        {"04 · Evitar contagem dupla", "Cache é parte da entrada e raciocínio, quando informado, é parte da saída. Ambos aparecem para análise, sem nova soma no total."},
        {"05 · Estimativa separada", "JTokkit estima tokens de arquivos e prompt identificados com a codificação O200K_BASE. Ela não representa necessariamente o tokenizer das outras IAs. A simulação de retirar skills, agents, specs e hooks é um cenário aproximado, não uma economia comprovada."},
        {"06 · Dados auditáveis", "Eventos brutos ficam em JSONL e os dados estruturados em SQLite. O painel confere a conta por eventos e sinaliza divergência, parcialidade ou ausência de uso."},
        {"07 · Execução em primeiro plano", "Enquanto a tarefa roda, operações recebem o foco visual. Depois, o painel devolve espaço às origens, ao simulador e ao total final."},
        {"08 · Distribuição Windows", "O pacote criado com jpackage inclui o runtime Java, para abrir o aplicativo sem depender do comando java no PATH."}
    };
    for (String[] item : items) add(page, card(item[0], item[1], BLUE));
    return page;
  }

  private static JPanel aiAndTokens(JDialog dialog, Store store, Store.Detail selected,
      Path projectRoot, Path sessionsRoot) {
    JPanel page = page("Construção com assistência de IA",
        "O criador definiu o objetivo, testou a experiência e refinou os requisitos. A implementação, os ajustes de interface, os testes e a documentação foram desenvolvidos iterativamente com assistência de IA.");
    TokenAccounting.Assessment selectedCount = selected == null ? null : TokenAccounting.assess(
        selected.run(), selected.run().totalTokens() > 0 || selected.run().inputTokens() + selected.run().outputTokens() > 0,
        selected.usageAuditable());

    JPanel development = ModalStyle.panel();
    development.setLayout(new BorderLayout(0, 12));
    development.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(ModalStyle.BORDER, 1), new EmptyBorder(14, 16, 14, 16)));
    development.add(label("TOKENS REGISTRADOS · CODEX NESTA PASTA", 13, Font.BOLD, ModalStyle.ACCENT), BorderLayout.NORTH);
    JPanel numbers = new JPanel(new GridLayout(1, 4, 9, 0));
    numbers.setOpaque(false);
    JLabel total = metric(numbers, "TOTAL DOS REGISTROS");
    JLabel input = metric(numbers, "ENTRADA");
    JLabel output = metric(numbers, "SAÍDA");
    JLabel cache = metric(numbers, "CACHE NA ENTRADA");
    JPanel cacheMetric = (JPanel) cache.getParent();
    cacheMetric.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    cacheMetric.setToolTipText("Clique para ver os registros e leituras que compõem esta soma.");
    MouseAdapter inspectCache = new MouseAdapter() {
      @Override public void mouseClicked(MouseEvent event) {
        DevelopmentCacheInspector.open(dialog, sessionsRoot, projectRoot);
      }
    };
    cacheMetric.addMouseListener(inspectCache);
    for (java.awt.Component component : cacheMetric.getComponents()) {
      component.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      component.addMouseListener(inspectCache);
    }
    development.add(numbers, BorderLayout.CENTER);
    JPanel noteRow = ModalStyle.panel();
    noteRow.setLayout(new BorderLayout(8, 0));
    JLabel note = label("Procurando registros locais...", 11, Font.PLAIN, MUTED);
    JButton refresh = ModalStyle.button("Atualizar contagem", false);
    noteRow.add(note, BorderLayout.CENTER);
    noteRow.add(refresh, BorderLayout.EAST);
    development.add(noteRow, BorderLayout.SOUTH);
    add(page, development);
    JPanel selectedTask = card("Tokens da tarefa selecionada no IAMonitor",
        selectedCount == null ? "Nenhuma sessão selecionada. Este número é medido por tarefa e não representa o desenvolvimento do programa."
            : selectedCount.total() + " tokens · " + selectedCount.status() + ". " + selectedCount.equation()
                + " Esta é uma tarefa monitorada, não a soma da criação do aplicativo.", BLUE);
    if (store != null && selected != null) {
      MouseAdapter inspectSession = new MouseAdapter() {
        @Override public void mouseClicked(MouseEvent event) {
          CacheInspector.open(dialog, store, selected, null);
        }
      };
      selectedTask.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      selectedTask.setToolTipText("Clique para inspecionar o cache da tarefa selecionada.");
      selectedTask.addMouseListener(inspectSession);
      for (java.awt.Component component : selectedTask.getComponents()) {
        component.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        component.addMouseListener(inspectSession);
      }
    }
    add(page, selectedTask);
    add(page, card("Como apresentar esse processo",
        "A trajetória visível neste projeto passou da ideia de monitoramento para uma aplicação Java desktop; depois vieram integração com várias CLIs, explicações de entrada e saída, simulação de economia, detalhes das origens e acompanhamento ao vivo. Cada revisão respondeu a uma necessidade de uso e demonstração.", ModalStyle.ACCENT));
    add(page, flow(new String[][]{
        {"1 · REQUISITOS", "O criador descreve o que precisa mostrar e medir."},
        {"2 · IMPLEMENTAÇÃO", "A IA propõe e altera o código Java do aplicativo."},
        {"3 · VERIFICAÇÃO", "Testes e prévias do Swing conferem o comportamento."},
        {"4 · REVISÃO", "O feedback orienta a próxima versão da interface."}
    }));
    add(page, card("Escopo e limite desta soma",
        "A leitura usa o último total cumulativo de cada registro JSONL do Codex iniciado exatamente nesta pasta de projeto. Soma sessões diferentes uma vez; cache já está na entrada. Não inclui outras IAs, conversas sem arquivo local ou sessões iniciadas em outra pasta; pode incluir atividades diferentes feitas nesta mesma pasta. Portanto, não é um total absoluto de toda a criação nem um valor de cobrança. Nenhum prompt ou credencial é exibido nesta tela.", BLUE));
    Runnable load = () -> {
      refresh.setEnabled(false);
      note.setText("Lendo contagens locais em segundo plano...");
      new SwingWorker<DevelopmentUsage.Breakdown, Void>() {
        @Override protected DevelopmentUsage.Breakdown doInBackground() throws Exception {
          return DevelopmentUsage.scanDetails(sessionsRoot, projectRoot);
        }
        @Override protected void done() {
          refresh.setEnabled(true);
          try {
            DevelopmentUsage.Result result = get().summary();
            if (result.sessions() == 0) {
              note.setText("Nenhum registro Codex iniciado nesta pasta foi encontrado.");
              return;
            }
            total.setText(num(result.total()));
            input.setText(num(result.input()));
            output.setText(num(result.output()));
            cache.setText(num(result.cached()));
            note.setText(result.sessions() + " registros incluídos · soma exata deste recorte, não de toda a criação.");
            note.setToolTipText(projectRoot + " · registros: " + sessionsRoot);
          } catch (Exception error) {
            note.setText("Não foi possível ler os registros locais: " + error.getMessage());
          }
        }
      }.execute();
    };
    refresh.addActionListener(event -> load.run());
    load.run();
    return page;
  }

  private static JLabel metric(JPanel row, String title) {
    JPanel box = new JPanel(new GridLayout(2, 1, 0, 3));
    box.setBackground(new Color(236, 242, 242));
    box.setBorder(new EmptyBorder(10, 11, 10, 11));
    box.add(label(title, 10, Font.BOLD, BLUE));
    JLabel value = label("—", 19, Font.BOLD, INK);
    box.add(value);
    row.add(box);
    return value;
  }

  private static JPanel flow(String[][] steps) {
    JPanel row = new JPanel(new GridLayout(1, steps.length, 8, 0));
    row.setOpaque(false);
    for (String[] step : steps) {
      JPanel item = new JPanel(new BorderLayout(0, 8));
      item.setBackground(new Color(233, 241, 243));
      item.setBorder(BorderFactory.createCompoundBorder(
          BorderFactory.createLineBorder(new Color(144, 181, 193), 1), new EmptyBorder(11, 11, 11, 11)));
      item.add(label(step[0], 11, Font.BOLD, BLUE), BorderLayout.NORTH);
      item.add(wrapped(step[1], 175), BorderLayout.CENTER);
      row.add(item);
    }
    return row;
  }

  private static JPanel page(String title, String intro) {
    JPanel page = ModalStyle.panel();
    page.setLayout(new GridBagLayout());
    page.setBorder(new EmptyBorder(20, 22, 20, 22));
    page.putClientProperty("nextRow", 0);
    add(page, label(title, 22, Font.BOLD, INK));
    add(page, wrapped(intro, 730));
    return page;
  }

  private static void add(JPanel page, java.awt.Component component) {
    int row = (int) page.getClientProperty("nextRow");
    GridBagConstraints c = new GridBagConstraints();
    c.gridx = 0; c.gridy = row; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
    c.anchor = GridBagConstraints.NORTHWEST; c.insets = new Insets(0, 0, 13, 0);
    page.add(component, c);
    page.putClientProperty("nextRow", row + 1);
  }

  private static JPanel card(String title, String body, Color accent) {
    JPanel card = new JPanel(new BorderLayout(0, 8));
    card.setBackground(PAPER);
    card.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createMatteBorder(0, 4, 1, 1, accent), new EmptyBorder(13, 15, 13, 15)));
    card.add(label(title, 15, Font.BOLD, accent), BorderLayout.NORTH);
    card.add(wrapped(body, 700), BorderLayout.CENTER);
    return card;
  }

  private static JTextArea wrapped(String text, int width) {
    JTextArea area = new JTextArea(text);
    area.setEditable(false);
    area.setFocusable(false);
    area.setOpaque(false);
    area.setLineWrap(true);
    area.setWrapStyleWord(true);
    area.setFont(new Font("Segoe UI", Font.PLAIN, 13));
    area.setForeground(INK);
    area.setCaretPosition(0);
    int columns = Math.max(18, width / 7);
    int rows = Math.max(2, (text.length() + columns - 1) / columns + 1);
    area.setPreferredSize(new Dimension(width, rows * area.getFontMetrics(area.getFont()).getHeight() + 2));
    return area;
  }

  private static JLabel label(String text, int size, int style, Color color) {
    JLabel label = new JLabel(text);
    label.setFont(new Font("Segoe UI", style, size));
    label.setForeground(color);
    return label;
  }

  private static JScrollPane scroll(JPanel page) {
    JScrollPane scroll = new JScrollPane(page);
    scroll.getVerticalScrollBar().setUnitIncrement(18);
    scroll.setBorder(null);
    scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    return scroll;
  }

  private static String num(long value) { return NUM.format(value); }
}

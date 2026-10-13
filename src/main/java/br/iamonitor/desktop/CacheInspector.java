package br.iamonitor.desktop;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

final class CacheInspector {
  private static final NumberFormat NUM = NumberFormat.getIntegerInstance(Locale.forLanguageTag("pt-BR"));
  private static final ObjectMapper JSON = new ObjectMapper();

  private CacheInspector() {}

  static void open(Window owner, Store store, Store.Detail selected, Integer sequence) {
    if (selected == null) {
      DetailsDialog.open(owner, "O que é cache de entrada", "Cache de entrada",
          "Selecione uma sessão para consultar valores observados", () ->
              "Cache de entrada é contexto de um pedido que o serviço de IA pode reutilizar em uma chamada posterior. "
              + "O modelo ainda gera uma nova saída.\n\n"
              + "Quando a CLI informa cache lido, essa quantidade faz parte da entrada total: "
              + "entrada sem cache + cache lido = entrada total.\n\n"
              + "Sem uma sessão selecionada não há contagens para inspecionar. Mesmo com uma sessão, "
              + "os eventos da CLI não revelam a sequência exata dos tokens, linhas ou arquivos que compuseram o cache.");
      return;
    }
    JDialog dialog = new JDialog(owner, "Inspeção do cache", java.awt.Dialog.ModalityType.APPLICATION_MODAL);
    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    dialog.setSize(1050, 730);
    dialog.setMinimumSize(new Dimension(830, 570));
    dialog.setLocationRelativeTo(owner);
    JLabel loading = label("Carregando resultados e eventos da sessão...", 17, Font.PLAIN);
    JPanel placeholder = ModalStyle.panel();
    placeholder.setLayout(new BorderLayout());
    placeholder.setBorder(new EmptyBorder(25, 30, 25, 30));
    placeholder.add(loading, BorderLayout.NORTH);
    dialog.setContentPane(placeholder);
    new SwingWorker<CacheEvidence.Snapshot, Void>() {
      @Override protected CacheEvidence.Snapshot doInBackground() throws Exception {
        Store.Detail detail;
        boolean complete;
        try {
          detail = store.reportDetail(selected.run().id());
          complete = detail != null;
        } catch (SQLException error) {
          detail = selected;
          complete = false;
        }
        if (detail == null) detail = selected;
        Map<Integer, String> payloads = new HashMap<>();
        for (Store.Event event : detail.events()) {
          if (!"turn.completed".equals(event.type())) continue;
          try { payloads.put(event.seq(), store.eventPayload(detail.run().id(), event.seq())); }
          catch (SQLException ignored) { /* The normalized count is still available. */ }
        }
        return CacheEvidence.from(detail, payloads, complete);
      }
      @Override protected void done() {
        if (!dialog.isDisplayable()) return;
        try {
          dialog.setContentPane(content(dialog, store, get(), sequence));
          dialog.revalidate();
          dialog.repaint();
        } catch (Exception error) {
          loading.setText("Não foi possível carregar os detalhes: " + error.getMessage());
        }
      }
    }.execute();
    ModalStyle.show(owner, dialog);
  }

  static JPanel content(Window owner, Store store, CacheEvidence.Snapshot evidence, Integer sequence) {
    JPanel root = ModalStyle.panel();
    root.setLayout(new BorderLayout(0, 12));
    root.setBorder(new EmptyBorder(18, 20, 17, 20));
    Store.Detail detail = evidence.detail();
    Store.Run run = detail.run();
    JPanel heading = ModalStyle.panel();
    heading.setLayout(new BorderLayout(0, 9));
    JPanel headingLines = ModalStyle.panel();
    headingLines.setLayout(new GridLayout(3, 1, 0, 5));
    headingLines.add(label("CACHE DE ENTRADA · " + Provider.name(run.provider()), 21, Font.BOLD));
    String headline = detail.usageAuditable()
        ? "Sessão: " + num(run.cachedTokens()) + " tokens em cache, dentro de "
            + num(run.inputTokens()) + " tokens de entrada"
        : "Contagem da sessão não auditável pelos eventos disponíveis";
    headingLines.add(label(headline, 17, Font.BOLD));
    headingLines.add(label("Cache reutiliza processamento de contexto de entrada; não repete uma resposta pronta.",
        14, Font.PLAIN));
    heading.add(headingLines, BorderLayout.NORTH);
    JTextArea location = textArea(locationAndMemory(store, run));
    location.setRows(5);
    location.setBorder(new EmptyBorder(6, 10, 6, 10));
    JPanel locationPanel = ModalStyle.panel();
    locationPanel.setLayout(new BorderLayout());
    locationPanel.setBorder(BorderFactory.createTitledBorder("Localização e memória · dados disponíveis"));
    locationPanel.add(location, BorderLayout.CENTER);
    heading.add(locationPanel, BorderLayout.CENTER);
    root.add(heading, BorderLayout.NORTH);

    DefaultTableModel unitsModel = new DefaultTableModel(
        new Object[]{"Resultado", "Horário", "Entrada", "Cache lido", "Saída"}, 0) {
      @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    for (CacheEvidence.Unit unit : evidence.units()) {
      EventLogic.Usage usage = unit.usage();
      unitsModel.addRow(new Object[]{unit.number(), time(unit.result().at()),
          usage == null ? "—" : num(usage.input()),
          usage == null ? "—" : num(usage.cached()),
          usage == null ? "—" : num(usage.output())});
    }
    JTable units = table(unitsModel);
    JScrollPane unitsScroll = new JScrollPane(units);
    unitsScroll.setBorder(BorderFactory.createTitledBorder("Resultados de uso · clique para inspecionar"));

    JPanel inspection = ModalStyle.panel();
    inspection.setLayout(new BorderLayout(0, 10));
    JTextArea numbers = textArea("Selecione um resultado de uso.");
    JScrollPane numbersScroll = new JScrollPane(numbers);
    numbersScroll.setPreferredSize(new Dimension(300, 145));
    numbersScroll.setBorder(BorderFactory.createTitledBorder("Composição reportada pela CLI"));
    inspection.add(numbersScroll, BorderLayout.NORTH);
    JPanel eventsPanel = ModalStyle.panel();
    eventsPanel.setLayout(new BorderLayout(0, 5));
    eventsPanel.add(label("EVENTOS REGISTRADOS NO INTERVALO · clique para abrir", 13, Font.BOLD),
        BorderLayout.NORTH);
    DefaultTableModel eventsModel = new DefaultTableModel(
        new Object[]{"Seq.", "Horário", "Evento observado"}, 0) {
      @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    JTable events = table(eventsModel);
    eventsPanel.add(new JScrollPane(events), BorderLayout.CENTER);
    inspection.add(eventsPanel, BorderLayout.CENTER);
    JButton raw = ModalStyle.button("Abrir JSON original do resultado", false);
    raw.setEnabled(false);
    inspection.add(raw, BorderLayout.SOUTH);
    JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, unitsScroll, inspection);
    split.setResizeWeight(0.43);
    split.setBorder(null);
    root.add(split, BorderLayout.CENTER);

    JTextArea limit = textArea("A CLI não informa quais linhas, arquivos ou tokens exatos formaram o prefixo em cache. "
        + "Eventos do intervalo não comprovam composição do cache; bytes da rede e cobrança não são medidos.");
    limit.setRows(2);
    limit.setBorder(new EmptyBorder(5, 10, 5, 10));
    JPanel footer = ModalStyle.panel();
    footer.setLayout(new BorderLayout(0, 8));
    footer.add(limit, BorderLayout.NORTH);
    JButton close = ModalStyle.button("Fechar", true);
    close.addActionListener(event -> {
      if (owner instanceof JDialog dialog) dialog.dispose();
    });
    JPanel buttons = ModalStyle.panel();
    buttons.add(close);
    footer.add(buttons, BorderLayout.SOUTH);
    root.add(footer, BorderLayout.SOUTH);

    units.getSelectionModel().addListSelectionListener(event -> {
      if (event.getValueIsAdjusting()) return;
      int row = units.getSelectedRow();
      if (row < 0) return;
      CacheEvidence.Unit unit = evidence.units().get(units.convertRowIndexToModel(row));
      numbers.setText(describe(unit, evidence));
      numbers.setCaretPosition(0);
      eventsModel.setRowCount(0);
      for (Store.Event observed : unit.interval())
        eventsModel.addRow(new Object[]{observed.seq(), time(observed.at()), observed.title()});
      raw.setEnabled(true);
    });
    raw.addActionListener(event -> {
      int row = units.getSelectedRow();
      if (row < 0) return;
      CacheEvidence.Unit unit = evidence.units().get(units.convertRowIndexToModel(row));
      openEvent(owner, store, run.id(), unit.result());
    });
    events.addMouseListener(new java.awt.event.MouseAdapter() {
      @Override public void mouseClicked(java.awt.event.MouseEvent event) {
        int row = events.rowAtPoint(event.getPoint());
        int selectedRow = units.getSelectedRow();
        if (row < 0 || selectedRow < 0) return;
        CacheEvidence.Unit unit = evidence.units().get(units.convertRowIndexToModel(selectedRow));
        Store.Event observed = unit.interval().get(events.convertRowIndexToModel(row));
        openEvent(owner, store, run.id(), observed);
      }
    });
    if (!evidence.units().isEmpty()) {
      int selected = 0;
      if (sequence != null) for (int i = 0; i < evidence.units().size(); i++) {
        CacheEvidence.Unit unit = evidence.units().get(i);
        if (unit.result().seq() == sequence || unit.startSequence() == sequence) {
          selected = i;
          break;
        }
      }
      units.setRowSelectionInterval(selected, selected);
    } else {
      numbers.setText("Nenhum resultado de uso foi registrado. Não há composição de cache disponível.");
    }
    return root;
  }

  static String describe(CacheEvidence.Unit unit, CacheEvidence.Snapshot evidence) {
    EventLogic.Usage usage = unit.usage();
    StringBuilder text = new StringBuilder("RESULTADO ").append(unit.number()).append(" · ")
        .append(unit.result().at()).append(" · evento ").append(unit.result().seq()).append("\n\n");
    if (usage == null) {
      text.append("A CLI não informou tokens neste resultado. Zero não é presumido.\n");
    } else if (unit.parts() == null) {
      text.append("A contagem deste resultado é divergente; não há decomposição confiável.\n")
          .append("JSON original disponível para inspeção.\n");
    } else {
      text.append("Entrada total: ").append(num(usage.input())).append(" tokens\n")
          .append("  Entrada sem cache: ").append(num(unit.parts().inputWithoutCache())).append(" tokens\n")
          .append("  Cache lido: ").append(num(usage.cached())).append(" tokens (parte da entrada)\n")
          .append("Saída: ").append(num(usage.output())).append(" tokens\n")
          .append("Total: ").append(num(usage.total())).append(" tokens\n");
      if (unit.parts().other() > 0)
        text.append("Sem detalhamento: ").append(num(unit.parts().other())).append(" tokens\n");
    }
    if (!unit.providerFields().isEmpty()) {
      text.append("\nCAMPOS ORIGINAIS DA CLI\n");
      for (CacheEvidence.Field field : unit.providerFields())
        text.append(field.label()).append(": ").append(num(field.tokens()))
            .append(" tokens  [").append(field.source()).append("]\n");
      if (unit.providerFields().stream().anyMatch(field -> field.label().startsWith("Cache criado")))
        text.append("Cache criado prepara contexto para possível reuso posterior; não integra o cache lido acima.\n");
    } else text.append("\nA CLI não forneceu um campo original adicional de cache neste registro.\n");
    text.append("\nEVENTOS NO INTERVALO: ").append(unit.interval().size()).append('\n')
        .append("São registros observados antes deste resultado; não identificam o conteúdo reaproveitado.\n")
        .append("Sequência exata dos tokens em cache: indisponível.\n");
    if (!evidence.completeHistory()) text.append("Histórico completo indisponível; eventos do intervalo podem faltar.\n");
    return text.toString();
  }

  static String locationAndMemory(Store store, Store.Run run) {
    Path events = store.rawFile(run.id()).toAbsolutePath().normalize();
    StringBuilder text = new StringBuilder()
        .append("Cache de prompt: caminho/arquivos físicos e RAM não informados pela CLI.\n");
    if ("claude".equals(run.provider()))
      text.append("API Claude: representações KV em memória; máquina e bytes não revelados.\n");
    else if ("codex".equals(run.provider()))
      text.append("API OpenAI: cache em máquinas do serviço; pode usar armazenamento na GPU.\n");
    else text.append("A CLI não revela se o provedor mantém este cache em RAM ou disco.\n");
    text.append("Registro de eventos do IAMonitor (não é o cache): ").append(events);
    if (!Files.isRegularFile(events)) text.append(" [arquivo ausente]");
    text.append("\nProcessos locais da CLI · pico de RAM amostrado: ");
    if (run.memoryPeak() > 0)
      text.append(String.format(Locale.forLanguageTag("pt-BR"), "%.1f MiB", run.memoryPeak() / 1048576.0))
          .append(" (inclui filhos; não mede o cache do provedor).");
    else text.append("sem amostras disponíveis; não significa zero de uso.");
    return text.toString();
  }

  private static void openEvent(Window owner, Store store, String id, Store.Event event) {
    DetailsDialog.open(owner, "Evento " + event.seq(), event.title(),
        "Sessão " + id.substring(0, 8).toUpperCase() + " · " + event.at(), () -> {
          StringBuilder description = new StringBuilder("Tipo: ").append(event.type())
              .append("\nClasse: ").append(event.kind())
              .append("\n\nRegistro observado; não há atribuição deste conteúdo ao cache.\n\n");
          if (event.detail() != null && !event.detail().isBlank())
            description.append("Resumo capturado:\n").append(event.detail()).append("\n\n");
          try {
            String raw = store.eventPayload(id, event.seq());
            if (raw != null) description.append("JSON original:\n")
                .append(JSON.readTree(raw).toPrettyString());
            else description.append("JSON original indisponível.");
          } catch (Exception error) {
            description.append("JSON original indisponível: ").append(error.getMessage());
          }
          return description.toString();
        });
  }

  private static JTable table(DefaultTableModel model) {
    JTable table = new JTable(model);
    table.setRowHeight(31);
    table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
    table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
    table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
    table.setAutoCreateRowSorter(true);
    return table;
  }

  private static JTextArea textArea(String text) {
    JTextArea area = new JTextArea(text);
    area.setEditable(false);
    area.setLineWrap(true);
    area.setWrapStyleWord(true);
    area.setFont(new Font("Segoe UI", Font.PLAIN, 14));
    area.setBackground(ModalStyle.PAPER);
    area.setForeground(ModalStyle.INK);
    area.setBorder(new EmptyBorder(12, 14, 12, 14));
    return area;
  }

  private static JLabel label(String text, int size, int style) {
    JLabel label = new JLabel(text);
    label.setFont(new Font("Segoe UI", style, size));
    label.setForeground(ModalStyle.INK);
    return label;
  }

  private static String num(long value) { return NUM.format(value); }
  private static String time(String value) {
    return value == null || value.length() < 19 ? "—" : value.substring(11, 19);
  }
}

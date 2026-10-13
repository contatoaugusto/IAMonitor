package br.iamonitor.desktop;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
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

final class DevelopmentCacheInspector {
  private static final NumberFormat NUM = NumberFormat.getIntegerInstance(Locale.forLanguageTag("pt-BR"));

  private DevelopmentCacheInspector() {}

  static void open(Window owner, Path sessionsRoot, Path projectRoot) {
    JDialog dialog = new JDialog(owner, "Cache nos registros locais de desenvolvimento",
        java.awt.Dialog.ModalityType.APPLICATION_MODAL);
    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    dialog.setSize(940, 640);
    dialog.setMinimumSize(new Dimension(720, 500));
    dialog.setLocationRelativeTo(owner);
    JLabel loading = new JLabel("Lendo registros locais...");
    loading.setFont(new Font("Segoe UI", Font.PLAIN, 16));
    JPanel placeholder = ModalStyle.panel();
    placeholder.setBorder(new EmptyBorder(25, 30, 25, 30));
    placeholder.add(loading);
    dialog.setContentPane(placeholder);
    new SwingWorker<DevelopmentUsage.Breakdown, Void>() {
      @Override protected DevelopmentUsage.Breakdown doInBackground() throws Exception {
        return DevelopmentUsage.scanDetails(sessionsRoot, projectRoot);
      }
      @Override protected void done() {
        if (!dialog.isDisplayable()) return;
        try {
          dialog.setContentPane(content(dialog, get(), sessionsRoot));
          dialog.revalidate();
          dialog.repaint();
        } catch (Exception error) {
          loading.setText("Não foi possível ler os registros: " + error.getMessage());
        }
      }
    }.execute();
    ModalStyle.show(owner, dialog);
  }

  static JPanel content(Window owner, DevelopmentUsage.Breakdown breakdown, Path sessionsRoot) {
    JPanel root = ModalStyle.panel();
    root.setLayout(new BorderLayout(0, 12));
    root.setBorder(new EmptyBorder(18, 20, 18, 20));
    JPanel heading = ModalStyle.panel();
    heading.setLayout(new BorderLayout(0, 6));
    JLabel title = new JLabel("CACHE DE DESENVOLVIMENTO · " + num(breakdown.summary().cached()) + " tokens");
    title.setFont(new Font("Segoe UI", Font.BOLD, 20));
    heading.add(title, BorderLayout.NORTH);
    JTextArea explanation = note("Soma do último total cumulativo de " + breakdown.summary().sessions()
        + " registros JSONL do Codex iniciados nesta pasta. É independente da sessão monitorada. "
        + "Cache integra a entrada. Os JSONL são registros locais, não arquivos do cache do provedor. "
        + "O caminho físico e a RAM do cache não são informados pela CLI.");
    explanation.setRows(4);
    heading.add(explanation, BorderLayout.SOUTH);
    root.add(heading, BorderLayout.NORTH);

    DefaultTableModel records = model("Registro local", "Cache", "Entrada", "Total");
    for (DevelopmentUsage.Entry entry : breakdown.entries()) {
      Path file = entry.file();
      String name = file.startsWith(sessionsRoot) ? sessionsRoot.relativize(file).toString() : file.toString();
      records.addRow(new Object[]{name, num(entry.last().cached()),
          num(entry.last().input()), num(entry.last().total())});
    }
    JTable recordsTable = table(records);
    JScrollPane recordScroll = new JScrollPane(recordsTable);
    recordScroll.setBorder(BorderFactory.createTitledBorder("Registros incluídos · clique para ver as leituras"));

    DefaultTableModel readings = model("Linha JSONL", "Entrada acumulada", "Cache acumulado", "Variação", "Total");
    JTable readingsTable = table(readings);
    JScrollPane readingsScroll = new JScrollPane(readingsTable);
    readingsScroll.setBorder(BorderFactory.createTitledBorder("Leituras de tokens · clique para detalhar"));
    JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, recordScroll, readingsScroll);
    split.setResizeWeight(0.48);
    root.add(split, BorderLayout.CENTER);
    JTextArea limitation = note("Cada linha mostra uma leitura cumulativa, não novos tokens a somar. "
        + "A variação é a diferença entre duas leituras válidas do mesmo registro; não identifica o texto em cache.");
    limitation.setRows(2);
    limitation.setPreferredSize(new Dimension(800, 48));
    root.add(limitation, BorderLayout.SOUTH);
    recordsTable.getSelectionModel().addListSelectionListener(event -> {
      if (event.getValueIsAdjusting()) return;
      int row = recordsTable.getSelectedRow();
      readings.setRowCount(0);
      if (row < 0) return;
      DevelopmentUsage.Entry entry = breakdown.entries().get(row);
      long previous = 0;
      for (DevelopmentUsage.Sample sample : entry.samples()) {
        String difference = sample.cached() >= previous ? "+" + num(sample.cached() - previous) : "—";
        readings.addRow(new Object[]{sample.line(), num(sample.input()),
            num(sample.cached()), difference, num(sample.total())});
        previous = sample.cached();
      }
    });
    readingsTable.addMouseListener(new java.awt.event.MouseAdapter() {
      @Override public void mouseClicked(java.awt.event.MouseEvent event) {
        int recordRow = recordsTable.getSelectedRow();
        int sampleRow = readingsTable.rowAtPoint(event.getPoint());
        if (recordRow < 0 || sampleRow < 0) return;
        DevelopmentUsage.Entry entry = breakdown.entries().get(recordRow);
        DevelopmentUsage.Sample sample = entry.samples().get(sampleRow);
        DetailsDialog.open(owner, "Leitura do registro local", "Linha " + sample.line(),
            entry.file().getFileName().toString(), () -> "Registro local (não é o cache): "
                + entry.file().toAbsolutePath().normalize()
                + "\nLinha JSONL: " + sample.line()
                + "\nEntrada acumulada: " + num(sample.input())
                + "\nCache acumulado: " + num(sample.cached())
                + "\nSaída acumulada: " + num(sample.output())
                + "\nTotal acumulado: " + num(sample.total())
                + "\n\nEsta linha é uma leitura de uso. O registro não atribui os tokens em cache a trechos, linhas ou arquivos do contexto.");
      }
    });
    if (!breakdown.entries().isEmpty()) recordsTable.setRowSelectionInterval(0, 0);
    return root;
  }

  private static JTable table(DefaultTableModel model) {
    JTable table = new JTable(model);
    table.setRowHeight(30);
    table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
    table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
    table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
    return table;
  }

  private static DefaultTableModel model(String... columns) {
    return new DefaultTableModel(columns, 0) {
      @Override public boolean isCellEditable(int row, int column) { return false; }
    };
  }

  private static JTextArea note(String text) {
    JTextArea area = new JTextArea(text);
    area.setLineWrap(true);
    area.setWrapStyleWord(true);
    area.setEditable(false);
    area.setOpaque(false);
    area.setFont(new Font("Segoe UI", Font.PLAIN, 14));
    return area;
  }

  private static String num(long value) { return NUM.format(value); }
}

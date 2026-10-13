package br.iamonitor.desktop;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Cursor;
import java.awt.RenderingHints;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.BorderFactory;
import javax.swing.border.EmptyBorder;

final class WorkbenchCharts {
  static final Color INPUT = new Color(87, 184, 224);
  static final Color CACHE = new Color(178, 149, 226);
  static final Color OUTPUT = new Color(243, 169, 101);
  static final Color OTHER = new Color(146, 161, 168);
  static final Color ARTIFACT = new Color(104, 204, 170);
  private static final Color PAPER = new Color(25, 54, 68);
  private static final Color INK = new Color(239, 243, 242);
  private static final Color MUTED = new Color(177, 198, 207);
  private static final NumberFormat NUM = NumberFormat.getIntegerInstance(Locale.forLanguageTag("pt-BR"));

  private WorkbenchCharts() {}

  static final class Usage extends JPanel {
    private WorkbenchMetrics.Snapshot data;
    private final JPanel cards = new CardList();
    private final JScrollPane scroll;
    private IntConsumer onCacheSelected = ignored -> {};

    Usage() {
      setBackground(PAPER);
      setPreferredSize(new Dimension(420, 250));
      setLayout(new BorderLayout());
      JPanel heading = new JPanel(new BorderLayout());
      heading.setBackground(PAPER);
      heading.setBorder(new EmptyBorder(9, 14, 4, 14));
      JLabel title = new JLabel("TOKENS POR CICLO");
      title.setForeground(INK);
      title.setFont(new Font("Segoe UI", Font.BOLD, 15));
      heading.add(title, BorderLayout.NORTH);
      add(heading, BorderLayout.NORTH);
      cards.setLayout(new BoxLayout(cards, BoxLayout.Y_AXIS));
      cards.setBackground(PAPER);
      scroll = new JScrollPane(cards);
      scroll.setBorder(BorderFactory.createEmptyBorder());
      scroll.getViewport().setBackground(PAPER);
      scroll.getVerticalScrollBar().setUnitIncrement(30);
      add(scroll, BorderLayout.CENTER);
      setData(WorkbenchMetrics.from(null));
    }

    void setData(WorkbenchMetrics.Snapshot value) {
      if (data != null && data.cycles().equals(value.cycles()) && data.auditable() == value.auditable()) return;
      data = value;
      cards.removeAll();
      if (value.cycles().isEmpty()) {
        JLabel empty = new JLabel("Aguardando ciclos registrados pela CLI");
        empty.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        empty.setForeground(MUTED);
        empty.setBorder(new EmptyBorder(20, 16, 0, 0));
        empty.setAlignmentX(LEFT_ALIGNMENT);
        cards.add(empty);
      } else {
        List<SessionFlow.Cycle> cycles = value.cycles();
        for (int i = cycles.size() - 1; i >= 0; i--) {
          CycleCard card = new CycleCard(i + 1, cycles.get(i), sequence -> onCacheSelected.accept(sequence));
          card.setAlignmentX(LEFT_ALIGNMENT);
          cards.add(card);
        }
      }
      cards.revalidate();
      cards.repaint();
      scroll.getVerticalScrollBar().setValue(0);
    }

    int cardCount() { return data.cycles().isEmpty() ? 0 : cards.getComponentCount(); }
    void onCacheSelected(IntConsumer action) { onCacheSelected = action; }
  }

  private static final class CardList extends JPanel implements Scrollable {
    @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
    @Override public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) { return 30; }
    @Override public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
      return Math.max(60, visible.height - 40);
    }
    @Override public boolean getScrollableTracksViewportWidth() { return true; }
    @Override public boolean getScrollableTracksViewportHeight() { return false; }
  }

  static final class CycleCard extends JPanel {
    record Metric(String label, long tokens, Color color) {}

    private final int index;
    private final SessionFlow.Cycle cycle;

    static List<Metric> metrics(EventLogic.Usage usage) {
      if (usage == null) return List.of();
      TokenAccounting.Parts parts = TokenAccounting.parts(usage.input(), usage.cached(),
          usage.output(), usage.reasoning(), usage.total());
      if (parts == null) return List.of();
      List<Metric> result = new ArrayList<>();
      result.add(new Metric("Entrada sem cache", parts.inputWithoutCache(), INPUT));
      result.add(new Metric("Cache", parts.cache(), CACHE));
      result.add(new Metric("Saída", parts.output(), OUTPUT));
      if (parts.other() > 0) result.add(new Metric("Outros", parts.other(), OTHER));
      return List.copyOf(result);
    }

    CycleCard(int index, SessionFlow.Cycle cycle) {
      this(index, cycle, ignored -> {});
    }

    CycleCard(int index, SessionFlow.Cycle cycle, IntConsumer onCacheSelected) {
      this.index = index;
      this.cycle = cycle;
      setBackground(new Color(32, 69, 82));
      int height = cycle.usage() == null || metrics(cycle.usage()).isEmpty()
          ? 68 : 30 + metrics(cycle.usage()).size() * 40 + 10;
      setPreferredSize(new Dimension(420, height));
      setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
      setBorder(BorderFactory.createMatteBorder(0, 0, 5, 0, PAPER));
      setToolTipText(cycle.usage() == null ? "A CLI não informou tokens para este ciclo."
          : metrics(cycle.usage()).isEmpty() ? "As contagens da CLI são divergentes neste ciclo."
          : "Entrada total: " + NUM.format(cycle.usage().input()) + " tokens, incluindo cache."
              + (cycle.usage().reasoning() > 0 ? " Raciocínio: "
                  + NUM.format(cycle.usage().reasoning()) + " tokens dentro da saída." : "")
              + " A contagem de tokens não determina cobrança da assinatura.");
      addMouseListener(new MouseAdapter() {
        @Override public void mouseClicked(MouseEvent event) {
          if (cacheAt(event.getY())) onCacheSelected.accept(cycle.seq());
        }
      });
      addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
        @Override public void mouseMoved(MouseEvent event) {
          setCursor(Cursor.getPredefinedCursor(cacheAt(event.getY())
              ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        }
      });
    }

    boolean cacheAt(int y) {
      return cycle.usage() != null && !metrics(cycle.usage()).isEmpty() && y >= 70 && y <= 110;
    }

    @Override public String getToolTipText(MouseEvent event) {
      if (cacheAt(event.getY())) return "Clique para inspecionar o cache deste ciclo.";
      return super.getToolTipText(event);
    }

    @Override protected void paintComponent(Graphics graphics) {
      super.paintComponent(graphics);
      Graphics2D g = (Graphics2D) graphics.create();
      setup(g);
      int width = getWidth();
      EventLogic.Usage usage = cycle.usage();
      List<Metric> metrics = metrics(usage);
      String state = usage == null ? cycle.completed() ? " · SEM CONTAGEM" : " · PENDENTE"
          : metrics.isEmpty() ? " · DIVERGENTE" : "";
      label(g, String.format(Locale.ROOT, "CICLO %02d", index) + state,
          18, 21, usage == null || metrics.isEmpty() ? MUTED : INK, 12, Font.BOLD);
      if (usage == null || metrics.isEmpty()) {
        label(g, usage == null ? "Tokens não informados" : "Contagem divergente da CLI",
            18, 47, MUTED, 13, Font.PLAIN);
      } else {
        String total = "Total " + NUM.format(usage.total());
        g.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label(g, total, Math.max(18, width - g.getFontMetrics().stringWidth(total) - 18),
            21, INK, 12, Font.BOLD);
        int barX = 18;
        int barWidth = Math.max(12, width - 36);
        long denominator = Math.max(1, usage.total());
        for (int i = 0; i < metrics.size(); i++) {
          Metric metric = metrics.get(i);
          int baseline = 48 + i * 40;
          label(g, metric.label(), 18, baseline, INK, 16, Font.BOLD);
          String value = value(metric);
          g.setFont(new Font("Segoe UI", Font.BOLD, 19));
          label(g, value, Math.max(120, width - g.getFontMetrics().stringWidth(value) - 18),
              baseline, metric.tokens() == 0 ? MUTED : INK, 19, Font.BOLD);
          g.setColor(new Color(52, 83, 95));
          g.fillRoundRect(barX, baseline + 7, barWidth, 13, 6, 6);
          int filled = (int) Math.round(barWidth * (double) metric.tokens() / denominator);
          if (metric.tokens() > 0) filled = Math.max(4, filled);
          if (filled > 0) {
            g.setColor(metric.color());
            g.fillRoundRect(barX, baseline + 7, Math.min(barWidth, filled), 13, 6, 6);
          }
        }
      }
      g.dispose();
    }

    static String value(Metric metric) {
      return NUM.format(metric.tokens()) + " tokens";
    }
  }

  static final class Artifacts extends JPanel {
    private WorkbenchMetrics.Snapshot data = WorkbenchMetrics.from(null);
    private final ArtifactCanvas canvas = new ArtifactCanvas();
    private Consumer<String> onCategorySelected = ignored -> {};

    Artifacts() {
      setBackground(PAPER);
      setPreferredSize(new Dimension(420, 250));
      setLayout(new BorderLayout());
      JPanel heading = new JPanel(new BorderLayout(0, 3));
      heading.setBackground(PAPER);
      heading.setBorder(new EmptyBorder(8, 12, 6, 12));
      JLabel title = new JLabel("ARTEFATOS IDENTIFICADOS");
      title.setForeground(INK);
      title.setFont(new Font("Segoe UI", Font.BOLD, 15));
      JLabel explanation = new JLabel("Tokens estimados localmente · clique para ver arquivos");
      explanation.setForeground(MUTED);
      explanation.setFont(new Font("Segoe UI", Font.PLAIN, 13));
      heading.add(title, BorderLayout.NORTH);
      heading.add(explanation, BorderLayout.SOUTH);
      add(heading, BorderLayout.NORTH);
      JScrollPane scroll = new JScrollPane(canvas);
      scroll.setBorder(BorderFactory.createEmptyBorder());
      scroll.getViewport().setBackground(PAPER);
      scroll.getVerticalScrollBar().setUnitIncrement(48);
      add(scroll, BorderLayout.CENTER);
      canvas.setToolTipText("");
      canvas.addMouseListener(new MouseAdapter() {
        @Override public void mouseClicked(MouseEvent event) {
          WorkbenchMetrics.Category category = canvas.categoryAt(event.getX(), event.getY());
          if (category != null) onCategorySelected.accept(category.name());
        }
      });
      canvas.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
        @Override public void mouseMoved(MouseEvent event) {
          canvas.setCursor(Cursor.getPredefinedCursor(canvas.categoryAt(event.getX(), event.getY()) == null
              ? Cursor.DEFAULT_CURSOR : Cursor.HAND_CURSOR));
        }
      });
      canvas.addComponentListener(new ComponentAdapter() {
        @Override public void componentResized(ComponentEvent event) { canvas.revalidate(); }
      });
    }

    void setData(WorkbenchMetrics.Snapshot value) {
      data = value;
      canvas.revalidate();
      canvas.repaint();
    }

    void onCategorySelected(Consumer<String> action) { onCategorySelected = action; }

    private final class ArtifactCanvas extends JPanel implements Scrollable {
      private static final int ROW_HEIGHT = 48;
      private static final int MARGIN = 10;
      private static final int GAP = 8;

      ArtifactCanvas() { setBackground(PAPER); }

      private int columns() { return getWidth() >= 460 ? 2 : 1; }
      private int rows() { return Math.max(1, (data.categories().size() + columns() - 1) / columns()); }

      @Override public Dimension getPreferredSize() {
        int viewportWidth = getParent() == null ? 420 : getParent().getWidth();
        int columns = viewportWidth >= 460 ? 2 : 1;
        int rows = Math.max(1, (data.categories().size() + columns - 1) / columns);
        return new Dimension(Math.max(420, viewportWidth), rows * ROW_HEIGHT + 4);
      }

      WorkbenchMetrics.Category categoryAt(int x, int y) {
        int columns = columns();
        int cardWidth = (getWidth() - 2 * MARGIN - (columns - 1) * GAP) / columns;
        int col = (x - MARGIN) / (cardWidth + GAP);
        int row = (y - 4) / ROW_HEIGHT;
        if (x < MARGIN || y < 4 || col < 0 || col >= columns || row < 0 || row >= rows()) return null;
        int left = MARGIN + col * (cardWidth + GAP);
        int top = 4 + row * ROW_HEIGHT;
        if (x >= left + cardWidth || y >= top + ROW_HEIGHT - 4) return null;
        int index = col * rows() + row;
        return index < data.categories().size() ? data.categories().get(index) : null;
      }

      @Override public String getToolTipText(MouseEvent event) {
        WorkbenchMetrics.Category category = categoryAt(event.getX(), event.getY());
        return category == null ? null : category.name() + ": " + category.files()
            + (category.files() == 1 ? " arquivo" : " arquivos") + ", "
            + NUM.format(category.estimatedTokens()) + " tokens estimados localmente. Clique para ver os arquivos.";
      }

      @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        setup(g);
        int columns = columns();
        int rows = rows();
        int cardWidth = (getWidth() - 2 * MARGIN - (columns - 1) * GAP) / columns;
        long max = Math.max(1, data.categories().stream()
            .mapToLong(WorkbenchMetrics.Category::estimatedTokens).max().orElse(0));
        for (int i = 0; i < data.categories().size(); i++) {
          WorkbenchMetrics.Category category = data.categories().get(i);
          int col = i / rows, row = i % rows;
          int left = MARGIN + col * (cardWidth + GAP), top = 4 + row * ROW_HEIGHT;
          g.setColor(new Color(32, 69, 82));
          g.fillRoundRect(left, top, cardWidth, ROW_HEIGHT - 4, 8, 8);
          label(g, category.name(), left + 10, top + 18,
              category.files() == 0 ? MUTED : INK, 16, Font.BOLD);
          String files = category.files() + (category.files() == 1 ? " arquivo" : " arquivos");
          g.setFont(new Font("Segoe UI", Font.PLAIN, 13));
          label(g, files, left + cardWidth - 10 - g.getFontMetrics().stringWidth(files),
              top + 18, category.files() == 0 ? MUTED : INK, 13, Font.PLAIN);
          String tokens = (category.files() == 0 ? "0" : "≈" + NUM.format(category.estimatedTokens())) + " tokens";
          g.setFont(new Font("Segoe UI", Font.BOLD, 14));
          int textWidth = g.getFontMetrics().stringWidth(tokens);
          int barX = left + 10;
          int barWidth = Math.max(12, cardWidth - 30 - textWidth);
          g.setColor(new Color(52, 83, 95));
          g.fillRoundRect(barX, top + 29, barWidth, 9, 5, 5);
          int filled = (int) Math.round(barWidth * (double) category.estimatedTokens() / max);
          if (filled > 0) {
            g.setColor(ARTIFACT);
            g.fillRoundRect(barX, top + 29, Math.max(3, filled), 9, 5, 5);
          }
          label(g, tokens, left + cardWidth - 10 - textWidth, top + 38,
              category.files() == 0 ? MUTED : INK, 14, Font.BOLD);
        }
        g.dispose();
      }

      @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
      @Override public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) { return ROW_HEIGHT; }
      @Override public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
        return Math.max(ROW_HEIGHT, visible.height - ROW_HEIGHT);
      }
      @Override public boolean getScrollableTracksViewportWidth() { return true; }
      @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }
  }

  private static void setup(Graphics2D g) {
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setStroke(new BasicStroke(1));
  }
  private static void label(Graphics2D g, String text, int x, int y, Color color, int size, int style) {
    g.setFont(new Font("Segoe UI", style, size)); g.setColor(color); g.drawString(text, x, y);
  }
}

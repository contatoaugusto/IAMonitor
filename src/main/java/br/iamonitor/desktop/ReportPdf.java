package br.iamonitor.desktop;

import java.awt.Color;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

final class ReportPdf implements AutoCloseable {
  private static final PDType1Font REGULAR = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
  private static final PDType1Font BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
  private static final Color INK = new Color(23, 38, 56);
  private static final Color MUTED = new Color(82, 105, 125);
  private static final Color TEAL = new Color(8, 125, 134);
  private static final Color INPUT = new Color(45, 133, 178);
  private static final Color CACHE = new Color(136, 103, 188);
  private static final Color OUTPUT = new Color(195, 108, 49);
  private static final Color OTHER = new Color(134, 148, 155);
  private static final Color ARTIFACT = new Color(47, 143, 105);
  private static final NumberFormat NUM = NumberFormat.getIntegerInstance(Locale.forLanguageTag("pt-BR"));
  private final PDDocument document = new PDDocument();
  private PDPageContentStream stream;
  private float y;
  private final float left = 48;
  private final float width = PDRectangle.A4.getWidth() - 96;

  private ReportPdf() throws IOException { newPage(); }

  static void write(Store.Detail detail, Path target) throws IOException {
    write(detail, DataTrace.from(detail.run(), new Store.TraceEvents(List.of(), 0),
        detail.artifacts()), target);
  }

  static void write(Store.Detail detail, DataTrace.Snapshot trace, Path target) throws IOException {
    try (ReportPdf report = new ReportPdf()) {
      report.fill(detail, trace);
      report.stream.close();
      report.stream = null;
      report.footers(detail.run().id());
      PDDocumentInformation info = new PDDocumentInformation();
      info.setTitle("IAMonitor - Sessão " + detail.run().id());
      info.setAuthor("IAMonitor");
      report.document.setDocumentInformation(info);
      report.document.save(target.toFile());
    }
  }

  private void newPage() throws IOException {
    if (stream != null) stream.close();
    PDPage page = new PDPage(PDRectangle.A4);
    document.addPage(page);
    stream = new PDPageContentStream(document, page);
    y = page.getMediaBox().getHeight() - 48;
  }

  private void ensure(float height) throws IOException {
    if (y - height < 70) newPage();
  }

  private void text(String value, PDType1Font font, float size, Color color, float x, float baseline) throws IOException {
    stream.beginText();
    stream.setFont(font, size);
    stream.setNonStrokingColor(color);
    stream.newLineAtOffset(x, baseline);
    stream.showText(latin(value));
    stream.endText();
  }

  private void paragraph(String value, PDType1Font font, float size, Color color, float lineHeight) throws IOException {
    for (String part : value.split("\\R", -1)) {
      for (String line : wrap(part, font, size)) {
        ensure(lineHeight);
        text(line, font, size, color, left, y);
        y -= lineHeight;
      }
    }
  }

  private List<String> wrap(String value, PDType1Font font, float size) throws IOException {
    List<String> result = new ArrayList<>();
    if (value.isEmpty()) { result.add(""); return result; }
    StringBuilder line = new StringBuilder();
    for (String word : latin(value).split("\\s+")) {
      if (word.isEmpty()) continue;
      if (measure(word, font, size) > width) {
        if (!line.isEmpty()) { result.add(line.toString()); line.setLength(0); }
        for (char character : word.toCharArray()) {
          String next = line.toString() + character;
          if (measure(next, font, size) > width && !line.isEmpty()) {
            result.add(line.toString()); line.setLength(0);
          }
          line.append(character);
        }
      } else if (measure(line.isEmpty() ? word : line + " " + word, font, size) > width) {
        result.add(line.toString()); line.setLength(0); line.append(word);
      } else {
        if (!line.isEmpty()) line.append(' ');
        line.append(word);
      }
    }
    if (!line.isEmpty()) result.add(line.toString());
    return result;
  }

  private static float measure(String value, PDType1Font font, float size) throws IOException {
    return font.getStringWidth(value) * size / 1000;
  }

  private void section(String heading) throws IOException {
    ensure(85);
    y -= 17;
    text(heading, BOLD, 13, INK, left, y);
    y -= 9;
    stream.setStrokingColor(new Color(219, 229, 237));
    stream.moveTo(left, y); stream.lineTo(left + width, y); stream.stroke();
    y -= 18;
  }

  private void field(String name, String value) throws IOException {
    ensure(32);
    text(name.toUpperCase(Locale.ROOT), BOLD, 8, MUTED, left, y);
    y -= 15;
    paragraph(value, REGULAR, 10, INK, 13);
    y -= 7;
  }

  private void row(String label, String value) throws IOException {
    ensure(19);
    text(label, REGULAR, 9, MUTED, left, y);
    text(value, BOLD, 9, INK, left + width - measure(latin(value), BOLD, 9), y);
    y -= 20;
  }

  private void fill(Store.Detail detail, DataTrace.Snapshot trace) throws IOException {
    Store.Run run = detail.run();
    WorkbenchMetrics.Snapshot metrics = WorkbenchMetrics.from(detail);
    boolean reported = run.totalTokens() > 0 || run.inputTokens() + run.outputTokens() > 0
        || detail.events().stream().anyMatch(event -> event.usage() != null);
    TokenAccounting.Assessment count = TokenAccounting.assess(run, reported, detail.usageAuditable());
    banner(run, count);
    y -= 13;
    field("Sessão", run.id() + "  |  " + Provider.name(run.provider())
        + (run.model() == null || run.model().isBlank() ? "" : "  |  " + run.model()));
    field("Projeto e período", run.project() + "  |  " + run.startedAt() + " até "
        + (run.endedAt() == null ? "em andamento" : run.endedAt()));

    section("01  |  O que foi medido");
    paragraph(count.status() + ". " + count.equation(), REGULAR, 10, INK, 14);
    y -= 7;
    usageBar(run, reported && detail.usageAuditable());
    paragraph(count.included(), REGULAR, 8, MUTED, 11);
    paragraph("Fonte: eventos de uso da CLI nesta sessão. Tokens não equivalem automaticamente a custo ou saldo de assinatura.",
        REGULAR, 8, MUTED, 11);

    section("02  |  Caminho lógico dos tokens");
    flowDiagram(run, reported && detail.usageAuditable());
    paragraph("As setas representam etapas conceituais. A CLI não expõe o número de requisições internas, bytes de rede ou quais trechos de arquivo foram enviados.",
        REGULAR, 8, MUTED, 11);

    section("03  |  Fases reportadas pela CLI");
    paragraph(metrics.completed() + " resultados concluídos; " + metrics.withoutUsage()
        + " sem contagem; " + metrics.pending() + " pendentes. Cada faixa abaixo representa um resultado de uso observado.",
        REGULAR, 9, INK, 13);
    if (metrics.cycles().isEmpty()) paragraph("Nenhum ciclo identificável nos eventos disponíveis.", REGULAR, 9, MUTED, 13);
    int phase = 0;
    for (SessionFlow.Cycle cycle : metrics.cycles()) cycleBar(++phase, cycle);

    section("04  |  Artefatos identificados");
    paragraph("Estimativas locais por tipo de arquivo. Não representam tokens efetivamente atribuídos pela CLI e não entram no total reportado.",
        REGULAR, 9, MUTED, 13);
    artifactBars(metrics.categories());
    for (Store.Artifact artifact : detail.artifacts()) {
      ensure(28);
      paragraph(artifact.category() + "  |  " + artifact.file(), REGULAR, 8, INK, 11);
      paragraph("aprox. " + NUM.format(artifact.estimatedTokens()) + " tokens locais", REGULAR, 8, MUTED, 11);
      y -= 3;
    }

    section("05  |  Recursos do processo local");
    row("Pico de CPU", String.format(Locale.forLanguageTag("pt-BR"), "%.1f%%", run.cpuPeak()));
    row("Pico de memória", String.format(Locale.forLanguageTag("pt-BR"), "%.1f MB", run.memoryPeak() / 1048576.0));
    paragraph("CPU e memória medem o processo local acompanhado. Não medem os servidores do provedor.",
        REGULAR, 8, MUTED, 11);

    section("06  |  Dados do projeto e envio remoto");
    paragraph("O pedido foi entregue à CLI local. " + trace.commandsWithOutput()
        + " comandos com saída registrada estão na trilha observável; " + trace.artifacts().size()
        + " arquivos foram identificados localmente. Arquivo identificado não prova leitura ou envio.",
        REGULAR, 9, INK, 13);
    paragraph("O IAMonitor não intercepta HTTPS. O corpo exato enviado ao provedor e a parcela de tokens de cada arquivo são indisponíveis. Consulte Dados do projeto e envio no aplicativo e revise o JSONL antes de divulgar conteúdo.",
        REGULAR, 8, MUTED, 11);

    section("07  |  Registro cronológico completo");
    paragraph(detail.events().size() + " eventos preservados em ordem. Valores de entrada, cache e saída aparecem apenas nos eventos que os informaram.",
        REGULAR, 9, MUTED, 13);
    if (detail.events().isEmpty()) paragraph("Nenhum evento registrado.", REGULAR, 9, MUTED, 13);
    for (Store.Event event : detail.events()) {
      ensure(39);
      paragraph("#" + event.seq() + "  " + event.at() + "  " + event.title(), BOLD, 9, INK, 12);
      if (!event.detail().isBlank()) paragraph(event.detail(), REGULAR, 8, MUTED, 11);
      if (event.usage() != null) paragraph("CLI: " + NUM.format(event.usage().input()) + " entrada ("
          + NUM.format(event.usage().cached()) + " cache), " + NUM.format(event.usage().output())
          + " saída, " + NUM.format(event.usage().total()) + " total", REGULAR, 8, TEAL, 11);
      y -= 6;
    }
    section("08  |  Pedido e metodologia");
    field("Pedido integral", run.prompt());
    paragraph("Os arquivos identificados podem não ter sido enviados ao modelo. Cache é subconjunto da entrada; raciocínio, quando informado, é subconjunto da saída. Ausência de contagem não significa zero. O arquivo JSONL exportado permite auditar os eventos originais.",
        REGULAR, 8, MUTED, 11);
  }

  private void banner(Store.Run run, TokenAccounting.Assessment count) throws IOException {
    text("IAMONITOR  /  ESTUDO DE EXECUÇÃO", BOLD, 9, TEAL, left, y);
    y -= 30;
    text("Relatório da sessão", BOLD, 24, INK, left, y);
    y -= 18;
    text("Medições, fases e origem dos números", REGULAR, 10, MUTED, left, y);
    y -= 18;
    float top = y;
    box(left, top - 88, width, 88, INK);
    text("TOKENS REPORTADOS PELA CLI", BOLD, 9, Color.WHITE, left + 16, top - 22);
    text(count.total().replace("≥ ", "pelo menos "), BOLD, 27, Color.WHITE, left + 16, top - 57);
    String status = switch (run.status()) {
      case "completed" -> "CONCLUÍDA";
      case "running" -> "EM EXECUÇÃO";
      case "interrupted" -> "INTERROMPIDA";
      case "failed" -> "FALHOU";
      default -> run.status().toUpperCase(Locale.ROOT);
    };
    text(status, BOLD, 9, new Color(132, 222, 211),
        left + width - 115, top - 22);
    y = top - 88;
  }

  private void box(float x, float bottom, float w, float h, Color color) throws IOException {
    stream.setNonStrokingColor(color);
    stream.addRect(x, bottom, w, h);
    stream.fill();
  }

  private void usageBar(Store.Run run, boolean available) throws IOException {
    ensure(72);
    TokenAccounting.Parts breakdown = TokenAccounting.parts(run.inputTokens(), run.cachedTokens(),
        run.outputTokens(), run.reasoningTokens(), run.totalTokens());
    if (!available || breakdown == null) {
      paragraph("Decomposição indisponível ou divergente nos eventos auditáveis.", REGULAR, 9, MUTED, 13);
      return;
    }
    long total = Math.max(1, run.totalTokens());
    long[] parts = {breakdown.inputWithoutCache(), breakdown.cache(), breakdown.output(), breakdown.other()};
    Color[] colors = {INPUT, CACHE, OUTPUT, OTHER};
    float x = left;
    box(left, y - 17, width, 17, new Color(232, 237, 239));
    for (int i = 0; i < parts.length; i++) {
      float segment = width * parts[i] / (float) total;
      if (segment > 0) { box(x, y - 17, segment, 17, colors[i]); x += segment; }
    }
    y -= 34;
    legend(new String[]{"Entrada sem cache", "Cache", "Saída", "Outros"}, colors);
    y -= 13;
    paragraph(NUM.format(parts[0]) + " entrada sem cache + " + NUM.format(parts[1])
        + " cache + " + NUM.format(parts[2]) + " saída"
        + (parts[3] > 0 ? " + " + NUM.format(parts[3]) + " outros" : "")
        + " = " + NUM.format(run.totalTokens()), REGULAR, 9, INK, 13);
  }

  private void legend(String[] names, Color[] colors) throws IOException {
    float x = left;
    for (int i = 0; i < names.length; i++) {
      box(x, y - 2, 8, 8, colors[i]);
      text(names[i], REGULAR, 8, MUTED, x + 12, y);
      x += new float[]{132, 70, 65, 60}[i];
    }
    y -= 15;
  }

  private void flowDiagram(Store.Run run, boolean available) throws IOException {
    ensure(127);
    String[] names = {"Pedido e contexto", "CLI local", "Modelo remoto", "Resposta e ações"};
    String[] values = {"Origem local", "Prepara a execução",
        available ? NUM.format(run.inputTokens()) + " entrada" : "Entrada não informada",
        available ? NUM.format(run.outputTokens()) + " saída" : "Saída não informada"};
    float gap = 13, cell = (width - 3 * gap) / 4;
    float top = y;
    for (int i = 0; i < 4; i++) {
      float x = left + i * (cell + gap);
      box(x, top - 66, cell, 66, i == 2 ? new Color(222, 239, 244) : new Color(236, 241, 241));
      text(String.format(Locale.ROOT, "%02d", i + 1), BOLD, 9, TEAL, x + 9, top - 15);
      text(names[i], BOLD, 9, INK, x + 9, top - 34);
      text(values[i], REGULAR, 8, MUTED, x + 9, top - 50);
      if (i < 3) {
        stream.setStrokingColor(TEAL);
        stream.moveTo(x + cell + 2, top - 33);
        stream.lineTo(x + cell + gap - 3, top - 33);
        stream.stroke();
      }
    }
    y = top - 78;
    if (available) paragraph(NUM.format(run.cachedTokens()) + " de cache já estão na entrada; "
        + NUM.format(run.reasoningTokens()) + " de raciocínio já estão na saída.", REGULAR, 8, TEAL, 11);
  }

  private void cycleBar(int index, SessionFlow.Cycle cycle) throws IOException {
    ensure(51);
    EventLogic.Usage usage = cycle.usage();
    TokenAccounting.Parts breakdown = usage == null ? null : TokenAccounting.parts(
        usage.input(), usage.cached(), usage.output(), usage.reasoning(), usage.total());
    String heading = String.format(Locale.ROOT, "%02d", index) + "  " + cycle.at()
        + (usage == null ? cycle.completed() ? "  |  sem contagem" : "  |  pendente"
            : breakdown == null ? "  |  contagem divergente"
            : "  |  " + NUM.format(usage.total()) + " tokens reportados");
    text(heading, BOLD, 9, INK, left, y);
    y -= 13;
    if (breakdown != null) {
      float total = Math.max(1, usage.total());
      float x = left;
      box(x, y - 9, width, 9, new Color(232, 237, 239));
      long[] parts = {breakdown.inputWithoutCache(), breakdown.cache(), breakdown.output(), breakdown.other()};
      Color[] colors = {INPUT, CACHE, OUTPUT, OTHER};
      for (int i = 0; i < parts.length; i++) {
        float segment = width * parts[i] / total;
        if (segment > 0) { box(x, y - 9, segment, 9, colors[i]); x += segment; }
      }
      y -= 20;
      text(NUM.format(parts[0]) + " entrada sem cache  |  " + NUM.format(parts[1])
          + " cache  |  " + NUM.format(parts[2]) + " saída  |  "
          + cycle.messageEvents() + " eventos de mensagem",
          REGULAR, 8, MUTED, left, y);
    } else text(usage == null ? "Não há contagem para esta fase; zero não foi presumido."
        : "Parcelas divergentes; nenhuma soma foi presumida.", REGULAR, 8, MUTED, left, y);
    y -= 20;
  }

  private void artifactBars(List<WorkbenchMetrics.Category> categories) throws IOException {
    if (categories.stream().noneMatch(category -> category.files() > 0))
      paragraph("Nenhum arquivo identificado. As barras vazias indicam ausência de arquivo observado, não consumo zero de contexto.",
          REGULAR, 8, MUTED, 11);
    long max = Math.max(1, categories.stream().mapToLong(WorkbenchMetrics.Category::estimatedTokens)
        .max().orElse(0));
    for (WorkbenchMetrics.Category category : categories) {
      ensure(27);
      text(category.name() + "  (" + category.files() + ")", BOLD, 9, INK, left, y);
      String value = category.files() == 0 ? "0 arquivos" : "aprox. " + NUM.format(category.estimatedTokens());
      text(value, BOLD, 9, category.files() == 0 ? MUTED : ARTIFACT,
          left + width - measure(latin(value), BOLD, 9), y);
      y -= 9;
      box(left, y - 6, width, 6, new Color(232, 237, 239));
      float length = width * category.estimatedTokens() / (float) max;
      if (length > 0) box(left, y - 6, length, 6, ARTIFACT);
      y -= 19;
    }
    y -= 8;
  }

  private void footers(String id) throws IOException {
    for (int index = 0; index < document.getNumberOfPages(); index++) {
      PDPage page = document.getPage(index);
      try (PDPageContentStream footer = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true)) {
        footer.setStrokingColor(new Color(219, 229, 237));
        footer.moveTo(left, 52); footer.lineTo(left + width, 52); footer.stroke();
        footer.beginText(); footer.setFont(REGULAR, 8); footer.setNonStrokingColor(MUTED);
        footer.newLineAtOffset(left, 38); footer.showText("IAMonitor  |  " + id.substring(0, 8)); footer.endText();
        footer.beginText(); footer.setFont(REGULAR, 8); footer.setNonStrokingColor(MUTED);
        footer.newLineAtOffset(left + width - 30, 38); footer.showText((index + 1) + " / " + document.getNumberOfPages()); footer.endText();
      }
    }
  }

  private static String latin(String value) {
    var encoder = StandardCharsets.ISO_8859_1.newEncoder();
    StringBuilder result = new StringBuilder();
    value.codePoints().forEach(code -> {
      String character = new String(Character.toChars(code));
      result.append(code < 32 || (code >= 127 && code < 160) ? " " : encoder.canEncode(character) ? character : "?");
    });
    return result.toString();
  }

  @Override public void close() throws IOException {
    if (stream != null) stream.close();
    document.close();
  }
}

package br.iamonitor.desktop;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReportPdfTest {
  @Test void paginaRelatorioLongoSemPaginasVazias(@TempDir Path temp) throws Exception {
    String prompt = "Analise o projeto e explique as métricas da sessão. ".repeat(55);
    Store.Run run = new Store.Run("12345678-1234-1234-1234-123456789abc",
        "D:\\Desenvolvimento\\IAMonitor", prompt, "gpt-6.1-sol", "workspace-write", "codex",
        "completed", "2026-10-08T12:00:00Z", "2026-10-08T12:05:00Z", 0, null,
        10000, 2500, 2050, 600, 12050, 0, 17.5, 456789123);
    List<Store.Artifact> artifacts = new ArrayList<>();
    for (int i = 0; i < 12; i++) artifacts.add(new Store.Artifact(
        "D:\\Desenvolvimento\\IAMonitor\\src\\arquivo-exemplo-" + i + ".tsx",
        "Spec", "2026-10-08T12:00:00Z", 3200 + i, 800 + i, "a".repeat(64)));
    List<Store.Event> events = new ArrayList<>();
    events.add(new Store.Event(0, "2026-10-08T12:00:00Z", "turn.started", "model",
        "Turno iniciado", ""));
    events.add(new Store.Event(1, "2026-10-08T12:04:00Z", "turn.completed", "model",
        "Turno concluído", "Uso de tokens atualizado",
        new EventLogic.Usage(10000, 2500, 2050, 600, 12050)));
    for (int i = 0; i < 60; i++) events.add(new Store.Event(i + 1, "2026-10-08T12:00:00Z",
        "item.completed", "local", "Comando local concluído",
        "Leitura e análise de arquivos para preparar o relatório da sessão. Evento " + i + "."));
    Store.Detail detail = new Store.Detail(run, events, artifacts, List.of(),
        new EventLogic().sources(prompt, artifacts));
    Path file = temp.resolve("report.pdf");
    ReportPdf.write(detail, DataTrace.from(run, new Store.TraceEvents(List.of(), 0), artifacts), file);
    try (var document = Loader.loadPDF(file.toFile())) {
      assertTrue(document.getNumberOfPages() >= 3);
      String fullText = new PDFTextStripper().getText(document);
      for (String section : List.of("O que foi medido", "Caminho lógico dos tokens",
          "Fases reportadas", "Artefatos identificados", "Registro cronológico completo",
          "Pedido e metodologia", "Dados do projeto e envio remoto")) assertTrue(fullText.contains(section), section);
      assertTrue(fullText.contains("#60"), "o relatório deve incluir a cronologia completa");
      assertTrue(fullText.contains("cache"));
      assertTrue(fullText.contains("7.500 entrada sem cache"));
      assertTrue(fullText.contains("Command  (0)"));
      assertTrue(fullText.contains("0 arquivos"));
      PDFRenderer renderer = new PDFRenderer(document);
      for (int page = 1; page <= document.getNumberOfPages(); page++) {
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setStartPage(page);
        stripper.setEndPage(page);
        assertTrue(stripper.getText(document).length() > 60, "página " + page + " vazia");
        assertTrue(renderer.renderImageWithDPI(page - 1, 72).getWidth() > 500);
      }
      if (Boolean.getBoolean("iamonitor.preview")) {
        ImageIO.write(renderer.renderImageWithDPI(0, 120), "png", Path.of("target/report-preview-first.png").toFile());
        ImageIO.write(renderer.renderImageWithDPI(1, 120), "png", Path.of("target/report-preview-second.png").toFile());
        ImageIO.write(renderer.renderImageWithDPI(2, 120), "png", Path.of("target/report-preview-third.png").toFile());
        ImageIO.write(renderer.renderImageWithDPI(document.getNumberOfPages() - 1, 120), "png",
            Path.of("target/report-preview-last.png").toFile());
      }
    }
  }
}

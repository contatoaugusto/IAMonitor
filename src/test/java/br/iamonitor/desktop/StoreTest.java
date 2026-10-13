package br.iamonitor.desktop;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StoreTest {
  private final ObjectMapper json = new ObjectMapper();

  @Test void reiniciaEExcluiSessaoComSeusDados(@TempDir Path temp) throws Exception {
    Path project = Files.createDirectory(temp.resolve("project"));
    Files.writeString(project.resolve("spec.md"), "Especificação de teste.");
    try (Store store = new Store(temp.resolve("data"), new EventLogic())) {
      Store.Run run = store.begin(project, "Leia spec.md", "", "read-only");
      String id = run.id();
      store.observeArtifacts(id, "Leia spec.md", project);
      assertEquals("Especificação de teste.", store.artifactContent(id, project.resolve("spec.md").toString()));
      var event = json.readTree("{\"type\":\"turn.completed\",\"usage\":{\"input_tokens\":40,\"output_tokens\":10,\"total_tokens\":50}}");
      store.recordEvent(id, event, json.writeValueAsString(event));
      store.sample(id, 4, 2048);
      assertEquals(50, store.detail(id).events().getFirst().usage().total());
      assertTrue(Files.exists(store.rawFile(id)));

      store.restart(id);
      Store.Detail cleared = store.detail(id);
      assertEquals("running", cleared.run().status());
      assertEquals("Leia spec.md", cleared.run().prompt());
      assertEquals(0, cleared.run().totalTokens());
      assertTrue(cleared.events().isEmpty());
      assertTrue(cleared.artifacts().isEmpty());
      assertNull(store.artifactContent(id, project.resolve("spec.md").toString()));
      assertTrue(cleared.samples().isEmpty());
      assertFalse(Files.exists(store.rawFile(id)));

      store.recordEvent(id, event, json.writeValueAsString(event));
      assertEquals(50, store.run(id).totalTokens());
      assertEquals(1, store.detail(id).events().size());
      store.delete(id);
      assertNull(store.run(id));
      assertFalse(Files.exists(store.rawFile(id)));
    }
  }

  @Test void reconstituiContagemDeSessoesSalvasAPartirDosEventos(@TempDir Path temp) throws Exception {
    Path project = Files.createDirectory(temp.resolve("project"));
    Path data = temp.resolve("data");
    String id;
    try (Store store = new Store(data, new EventLogic())) {
      id = store.begin(project, "Pedido", "", "read-only").id();
      var valid = json.readTree("""
          {"type":"turn.completed","usage":{"input_tokens":120,"cached_input_tokens":20,
          "output_tokens":30,"total_tokens":150}}
          """);
      var incomplete = json.readTree("""
          {"type":"turn.completed","usage":{"input_tokens":80}}
          """);
      store.recordEvent(id, valid, json.writeValueAsString(valid));
      store.recordEvent(id, incomplete, json.writeValueAsString(incomplete));
      store.finish(id, "completed", 0, null);
    }
    try (var connection = DriverManager.getConnection("jdbc:sqlite:" + data.resolve("iamonitor.sqlite"));
         var statement = connection.createStatement()) {
      statement.executeUpdate("UPDATE runs SET input_tokens=999,total_tokens=999,missing_usage=0 WHERE id='" + id + "'");
    }
    try (Store reopened = new Store(data, new EventLogic())) {
      Store.Run audited = reopened.detail(id).run();
      assertEquals(120, audited.inputTokens());
      assertEquals(20, audited.cachedTokens());
      assertEquals(30, audited.outputTokens());
      assertEquals(150, audited.totalTokens());
      assertEquals(1, audited.missingUsage());
      assertEquals("≥ 150", TokenAccounting.assess(audited, true).total());
    }
  }
  @Test void reutilizaBancoExistenteEExportaRelatorio(@TempDir Path temp) throws Exception {
    Path project = Files.createDirectory(temp.resolve("project"));
    Files.writeString(project.resolve("AGENTS.md"), "Instruções para o projeto.");
    Files.writeString(project.resolve("spec.md"), "Especificação da sessão.");
    Path data = temp.resolve("data");
    String id;
    try (Store store = new Store(data, new EventLogic())) {
      Store.Run run = store.begin(project, "Leia \"spec.md\"", "", "read-only");
      id = run.id();
      store.observeArtifacts(id, "Leia \"spec.md\"", project);
      store.observeArtifacts(id, "\"" + project.resolve("AGENTS.md") + "\"", project);
      var command = json.readTree("{\"type\":\"item.completed\",\"item\":{\"type\":\"command_execution\",\"command\":\"Get-Content spec.md\"}}");
      store.recordEvent(id, command, json.writeValueAsString(command));
      var completed = json.readTree("{\"type\":\"turn.completed\",\"usage\":{\"input_tokens\":120,\"cached_input_tokens\":20,\"output_tokens\":30,\"reasoning_tokens\":5,\"total_tokens\":150}}");
      store.recordEvent(id, completed, json.writeValueAsString(completed));
      store.sample(id, 12.5, 10485760);
      store.finish(id, "completed", 0, null);
      Store.Detail detail = store.detail(id);
      assertEquals(150, detail.run().totalTokens());
      assertEquals(20, detail.run().cachedTokens());
      assertEquals(2, detail.artifacts().size());
      assertEquals(2, detail.events().size());
      assertEquals("local", detail.events().getFirst().kind());
      assertTrue(detail.inputSources().get("agent").estimatedTokens() > 0);
      assertTrue(detail.inputSources().get("spec").estimatedTokens() > 0);
      assertEquals(1, detail.samples().size());
      assertTrue(Files.readString(store.rawFile(id)).contains("turn.completed"));

      Path pdf = temp.resolve("report.pdf");
      ReportPdf.write(detail, pdf);
      try (var document = Loader.loadPDF(pdf.toFile())) {
        assertTrue(document.getNumberOfPages() >= 1);
        String text = new PDFTextStripper().getText(document);
        assertTrue(text.contains("150"));
        assertTrue(text.contains("spec.md"));
        assertTrue(text.contains("O que foi medido"));
        for (int page = 1; page <= document.getNumberOfPages(); page++) {
          PDFTextStripper stripper = new PDFTextStripper();
          stripper.setStartPage(page);
          stripper.setEndPage(page);
          assertTrue(stripper.getText(document).length() > 60, "página " + page + " não está vazia");
        }
      }
    }
    try (Store reopened = new Store(data, new EventLogic())) {
      assertEquals("completed", reopened.run(id).status());
      assertEquals(150, reopened.detail(id).run().totalTokens());
    }
  }

  @Test void relatorioCarregaCronologiaCompletaAcimaDoLimiteDoPainel(@TempDir Path temp) throws Exception {
    Path project = Files.createDirectory(temp.resolve("project"));
    try (Store store = new Store(temp.resolve("data"), new EventLogic())) {
      Store.Run run = store.begin(project, "Estude a sequência", "", "read-only");
      String raw = "{\"type\":\"item.completed\",\"item\":{\"type\":\"command_execution\",\"command\":\"echo ok\"}}";
      var event = json.readTree(raw);
      for (int i = 0; i < 505; i++) store.recordEvent(run.id(), event, raw);
      assertEquals(500, store.detail(run.id()).events().size());
      Store.Detail report = store.reportDetail(run.id());
      assertEquals(505, report.events().size());
      assertEquals(1, report.events().getFirst().seq());
      assertEquals(505, report.events().getLast().seq());
    }
  }
}

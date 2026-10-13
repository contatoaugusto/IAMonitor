package br.iamonitor.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DataTraceTest {
  private final ObjectMapper json = new ObjectMapper();

  @Test void distingueSaidaLocalDeEnvioRemotoEArquivoIdentificado(@TempDir Path temp)
      throws Exception {
    Path project = Files.createDirectory(temp.resolve("projeto"));
    Files.writeString(project.resolve("Arquitetura.md"), "Camadas: HTTP, serviço e repositório.");
    try (Store store = new Store(temp.resolve("data"), new EventLogic())) {
      String id = store.begin(project, "Estude Arquitetura.md e crie cadastro", "",
          "workspace-write").id();
      record(store, id, """
          {"type":"item.completed","item":{"type":"command_execution",
           "command":"Get-Content Arquitetura.md",
           "aggregated_output":"Camadas: HTTP, serviço e repositório.","exit_code":0}}
          """);
      record(store, id, """
          {"type":"item.completed","item":{"type":"file_change",
           "changes":[{"path":"consumoTokensOnline/index.html","kind":"add"}]}}
          """);
      store.observeArtifacts(id, "Arquitetura.md", project);
      DataTrace.Snapshot snapshot = DataTrace.from(store.run(id), store.traceEvents(id),
          store.detail(id).artifacts());
      assertEquals(2, snapshot.totalEvidence());
      assertEquals(1, snapshot.commandsWithOutput());
      assertTrue(snapshot.evidence().getFirst().paths().getFirst().endsWith("Arquitetura.md"));
      String text = DataTrace.describe(snapshot);
      assertTrue(text.contains("Camadas: HTTP, serviço e repositório."));
      assertTrue(text.contains("Caminhos citados no comando (leitura não comprovada)"));
      assertTrue(text.contains("MUDANÇA DE ARQUIVO LOCAL"));
      assertTrue(text.contains("ENVIO REMOTO EXATO: INDISPONÍVEL"));
      assertFalse(text.contains("foi enviado à OpenAI"));
    }
  }

  @Test void ausenciaDeEventosNaoViraProvaDeEnvio(@TempDir Path temp) throws Exception {
    Path project = Files.createDirectory(temp.resolve("projeto"));
    try (Store store = new Store(temp.resolve("data"), new EventLogic())) {
      String id = store.begin(project, "Pedido", "", "read-only").id();
      var snapshot = DataTrace.from(store.run(id), store.traceEvents(id),
          store.detail(id).artifacts());
      assertEquals(0, snapshot.totalEvidence());
      assertTrue(DataTrace.describe(snapshot).contains("Nenhum comando ou mudança estruturada"));
      assertTrue(DataTrace.describe(snapshot).contains("corpo exato enviado"));
    }
  }

  @Test void limitaTelaSemPerderContagemOuJsonl(@TempDir Path temp) throws Exception {
    Path project = Files.createDirectory(temp.resolve("projeto"));
    try (Store store = new Store(temp.resolve("data"), new EventLogic())) {
      String id = store.begin(project, "Pedido", "", "read-only").id();
      for (int i = 0; i < 402; i++) record(store, id, """
          {"type":"item.completed","item":{"type":"command_execution",
           "command":"rg --files","aggregated_output":"arquivo.java"}}
          """);
      var snapshot = DataTrace.from(store.run(id), store.traceEvents(id),
          store.detail(id).artifacts());
      assertEquals(402, snapshot.totalEvidence());
      assertEquals(400, snapshot.evidence().size());
      assertTrue(snapshot.limited());
      assertTrue(DataTrace.describe(snapshot).contains("Consulte o JSONL"));
      assertEquals(402, Files.readAllLines(store.rawFile(id)).size());
    }
  }

  private void record(Store store, String id, String payload) throws Exception {
    var event = json.readTree(payload);
    store.recordEvent(id, event, json.writeValueAsString(event));
  }
}
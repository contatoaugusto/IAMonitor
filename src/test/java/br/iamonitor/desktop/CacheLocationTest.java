package br.iamonitor.desktop;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CacheLocationTest {
  @Test void separaRegistroLocalDaMemoriaDoCache(@TempDir Path temp) throws Exception {
    Path project = Files.createDirectory(temp.resolve("project"));
    try (Store store = new Store(temp.resolve("data"), new EventLogic())) {
      Store.Run run = store.begin(project, "Pedido fictício", "", "read-only");
      String before = CacheInspector.locationAndMemory(store, run);
      assertTrue(before.contains("caminho/arquivos físicos e RAM não informados pela CLI"));
      assertTrue(before.contains(store.rawFile(run.id()).toAbsolutePath().toString()));
      assertTrue(before.contains("não é o cache"));
      assertTrue(before.contains("sem amostras disponíveis; não significa zero"));
      assertFalse(before.contains("0,0 MiB"));

      store.sample(run.id(), 3, 157286400);
      String after = CacheInspector.locationAndMemory(store, store.reportDetail(run.id()).run());
      assertTrue(after.contains("150,0 MiB"));
      assertTrue(after.contains("não mede o cache do provedor"));
      assertTrue(after.contains("RAM não informados pela CLI"));
    }
  }

  @Test void naoExtrapolaCacheClaudeParaMemoriaDaSessao(@TempDir Path temp) throws Exception {
    Path project = Files.createDirectory(temp.resolve("project"));
    try (Store store = new Store(temp.resolve("data"), new EventLogic())) {
      Store.Run codex = store.begin(project, "Pedido fictício", "", "read-only");
      Store.Run claude = store.begin(project, "Pedido fictício", "", "read-only", "claude");
      String description = CacheInspector.locationAndMemory(store, claude);
      assertTrue(description.contains("API Claude: representações KV em memória"));
      assertTrue(description.contains("máquina e bytes não revelados"));
      assertFalse(CacheInspector.locationAndMemory(store, codex).contains("API Claude:"));
    }
  }
}

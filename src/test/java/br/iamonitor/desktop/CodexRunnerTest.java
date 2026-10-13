package br.iamonitor.desktop;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CodexRunnerTest {
  @Test void executaCodexECapturaEventos(@TempDir Path temp) throws Exception {
    Path project = Files.createDirectory(temp.resolve("project"));
    Files.writeString(project.resolve("README.md"), "Teste de captura.");
    Path java = Path.of(System.getProperty("java.home"), "bin",
        System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java");
    List<String> command = List.of(java.toString(), "-cp", System.getProperty("java.class.path"),
        FixtureCodex.class.getName());
    AtomicInteger changes = new AtomicInteger();
    try (Store store = new Store(temp.resolve("data"), new EventLogic());
         CodexRunner runner = new CodexRunner(store, changes::incrementAndGet, command)) {
      Store.Run created = runner.start(project, "Leia README.md", "", "read-only");
      Store.Run completed = await(store, created.id(), "completed");
      assertEquals(150, completed.totalTokens());
      assertEquals(20, completed.cachedTokens());
      assertTrue(store.detail(created.id()).events().stream().anyMatch(e -> e.title().equals("Comando local concluído")));
      assertTrue(store.detail(created.id()).artifacts().stream().anyMatch(a -> a.file().endsWith("README.md")));
      assertTrue(changes.get() > 0);

      Store.Run restarted = runner.restart(created.id());
      assertEquals(created.id(), restarted.id());
      assertEquals(150, await(store, created.id(), "completed").totalTokens());
      assertEquals(4, store.detail(created.id()).events().size());

      Store.Run sleeping = runner.start(project, "sleep README.md", "", "read-only");
      runner.stop(sleeping.id());
      assertEquals("interrupted", await(store, sleeping.id(), "interrupted").status());
    }
  }

  @Test void consultaCotaAntesEDepoisSemAfetarExecucao(@TempDir Path temp) throws Exception {
    Path project = Files.createDirectory(temp.resolve("project"));
    Path java = Path.of(System.getProperty("java.home"), "bin",
        System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java");
    List<String> command = List.of(java.toString(), "-cp", System.getProperty("java.class.path"),
        FixtureCodex.class.getName());
    CodexQuotaReader quota = new CodexQuotaReader(List.of(java.toString(), "-cp",
        System.getProperty("java.class.path"), FixtureQuota.class.getName()));
    try (Store store = new Store(temp.resolve("data"), new EventLogic());
         CodexRunner runner = new CodexRunner(store, () -> {}, command, quota)) {
      Store.Run run = runner.start(project, "Teste de cota", "", "read-only");
      assertEquals(1, store.quotaSnapshots(run.id()).size());
      await(store, run.id(), "completed");
      for (int i = 0; i < 100 && store.quotaSnapshots(run.id()).size() < 2; i++) Thread.sleep(100);
      assertEquals(2, store.quotaSnapshots(run.id()).size());
    }
  }

  private static Store.Run await(Store store, String id, String status) throws Exception {
    for (int i = 0; i < 100; i++) {
      Store.Run run = store.run(id);
      if (status.equals(run.status())) return run;
      Thread.sleep(100);
    }
    fail("A sessão não terminou como " + status + ": " + store.run(id).status());
    return null;
  }
}

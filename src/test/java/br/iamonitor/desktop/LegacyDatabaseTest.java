package br.iamonitor.desktop;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.Statement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LegacyDatabaseTest {
  @Test void abreBancoCriadoPeloPainelAnterior(@TempDir Path temp) throws Exception {
    Path data = Files.createDirectory(temp.resolve("data"));
    String id = "12345678-1234-1234-1234-123456789abc";
    try (var connection = DriverManager.getConnection("jdbc:sqlite:" + data.resolve("iamonitor.sqlite"));
         Statement statement = connection.createStatement()) {
      statement.execute("""
        CREATE TABLE runs (id TEXT PRIMARY KEY, project TEXT NOT NULL, prompt TEXT NOT NULL,
          model TEXT, sandbox TEXT NOT NULL, status TEXT NOT NULL, started_at TEXT NOT NULL,
          ended_at TEXT, exit_code INTEGER, error TEXT, input_tokens INTEGER NOT NULL DEFAULT 0,
          cached_tokens INTEGER NOT NULL DEFAULT 0, output_tokens INTEGER NOT NULL DEFAULT 0,
          reasoning_tokens INTEGER NOT NULL DEFAULT 0, total_tokens INTEGER NOT NULL DEFAULT 0,
          missing_usage INTEGER NOT NULL DEFAULT 0, cpu_peak REAL NOT NULL DEFAULT 0,
          memory_peak INTEGER NOT NULL DEFAULT 0)
        """);
      statement.execute("INSERT INTO runs (id,project,prompt,sandbox,status,started_at,input_tokens,total_tokens) "
          + "VALUES ('" + id + "','D:\\projeto','Pedido antigo','read-only','completed',"
          + "'2026-10-08T12:00:00.000Z',120,150)");
    }
    try (Store store = new Store(data, new EventLogic())) {
      assertEquals(1, store.runs().size());
      assertEquals("Pedido antigo", store.run(id).prompt());
      assertEquals("codex", store.run(id).provider());
      assertEquals(150, store.detail(id).run().totalTokens());
      assertTrue(store.detail(id).events().isEmpty());
      assertFalse(store.detail(id).usageAuditable());
      assertEquals("—", TokenAccounting.assess(store.detail(id).run(), true, false).total());
    }
  }
}

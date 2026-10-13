package br.iamonitor.desktop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class Store implements AutoCloseable {
  record Run(String id, String project, String prompt, String model, String sandbox, String provider, String status,
             String startedAt, String endedAt, Integer exitCode, String error, long inputTokens,
             long cachedTokens, long outputTokens, long reasoningTokens, long totalTokens,
             long missingUsage, double cpuPeak, long memoryPeak) {
    @Override public String toString() {
      Path path = Path.of(project);
      return path.getFileName() + "  ·  " + Provider.name(provider) + "  ·  " + status;
    }
  }
  record Event(int seq, String at, String type, String kind, String title, String detail, EventLogic.Usage usage) {
    Event(int seq, String at, String type, String kind, String title, String detail) {
      this(seq, at, type, kind, title, detail, null);
    }
  }
  record Artifact(String file, String category, String firstSeen, long bytes, long estimatedTokens, String sha256) {}
  record Sample(String at, double cpuPercent, long memoryBytes) {}
  record RawEvent(int seq, String at, String payload) {}
  record TraceEvents(List<RawEvent> items, int total) {}
  record QuotaSnapshot(String phase, String limitId, String window, int windowMinutes,
                       long resetsAt, double usedPercent, String capturedAt, String planType) {}
  record Detail(Run run, List<Event> events, List<Artifact> artifacts, List<Sample> samples,
                Map<String, EventLogic.Source> inputSources, boolean usageAuditable) {
    Detail(Run run, List<Event> events, List<Artifact> artifacts, List<Sample> samples,
           Map<String, EventLogic.Source> inputSources) {
      this(run, events, artifacts, samples, inputSources,
          events.stream().anyMatch(event -> "turn.completed".equals(event.type())));
    }
  }

  private static final ObjectMapper JSON = new ObjectMapper();
  private final Path dataDir;
  private final Connection db;
  private final EventLogic logic;

  Store(Path dataDir, EventLogic logic) throws SQLException, IOException {
    this.dataDir = dataDir.toAbsolutePath().normalize();
    this.logic = logic;
    Files.createDirectories(this.dataDir.resolve("runs"));
    db = DriverManager.getConnection("jdbc:sqlite:" + this.dataDir.resolve("iamonitor.sqlite"));
    try (Statement statement = db.createStatement()) {
      statement.execute("PRAGMA journal_mode=WAL");
      statement.executeUpdate("""
        CREATE TABLE IF NOT EXISTS runs (
          id TEXT PRIMARY KEY, project TEXT NOT NULL, prompt TEXT NOT NULL, model TEXT,
          sandbox TEXT NOT NULL, status TEXT NOT NULL, started_at TEXT NOT NULL,
          ended_at TEXT, exit_code INTEGER, error TEXT, input_tokens INTEGER NOT NULL DEFAULT 0,
          cached_tokens INTEGER NOT NULL DEFAULT 0, output_tokens INTEGER NOT NULL DEFAULT 0,
          reasoning_tokens INTEGER NOT NULL DEFAULT 0, total_tokens INTEGER NOT NULL DEFAULT 0,
          missing_usage INTEGER NOT NULL DEFAULT 0, cpu_peak REAL NOT NULL DEFAULT 0,
          memory_peak INTEGER NOT NULL DEFAULT 0, provider TEXT NOT NULL DEFAULT 'codex')
        """);
      if (db.getMetaData().getColumns(null, null, "runs", "provider").next() == false)
        statement.executeUpdate("ALTER TABLE runs ADD COLUMN provider TEXT NOT NULL DEFAULT 'codex'");
      statement.executeUpdate("""
        CREATE TABLE IF NOT EXISTS events (
          run_id TEXT NOT NULL, seq INTEGER NOT NULL, at TEXT NOT NULL, type TEXT NOT NULL,
          kind TEXT NOT NULL, title TEXT NOT NULL, detail TEXT NOT NULL, payload TEXT NOT NULL,
          PRIMARY KEY(run_id, seq))
        """);
      statement.executeUpdate("""
        CREATE TABLE IF NOT EXISTS artifacts (
          run_id TEXT NOT NULL, file TEXT NOT NULL, category TEXT NOT NULL,
          first_seen TEXT NOT NULL, bytes INTEGER NOT NULL, estimated_tokens INTEGER NOT NULL,
          sha256 TEXT NOT NULL, PRIMARY KEY(run_id, file))
        """);
      statement.executeUpdate("""
        CREATE TABLE IF NOT EXISTS artifact_snapshots (
          run_id TEXT NOT NULL, file TEXT NOT NULL, content TEXT NOT NULL,
          PRIMARY KEY(run_id, file))
        """);
      statement.executeUpdate("""
        CREATE TABLE IF NOT EXISTS samples (
          run_id TEXT NOT NULL, at TEXT NOT NULL, cpu_percent REAL NOT NULL,
          memory_bytes INTEGER NOT NULL)
        """);
      statement.executeUpdate("""
        CREATE TABLE IF NOT EXISTS quota_snapshots (
          run_id TEXT NOT NULL, phase TEXT NOT NULL, limit_id TEXT NOT NULL,
          window_name TEXT NOT NULL, window_minutes INTEGER NOT NULL,
          resets_at INTEGER NOT NULL, used_percent REAL NOT NULL,
          captured_at TEXT NOT NULL, plan_type TEXT,
          PRIMARY KEY(run_id, phase, limit_id, window_name))
        """);
    }
  }

  Path dataDirectory() { return dataDir; }

  Path rawFile(String id) {
    return dataDir.resolve("runs").resolve(id + ".jsonl");
  }

  synchronized List<Run> runs() throws SQLException {
    List<Run> result = new ArrayList<>();
    try (PreparedStatement statement = db.prepareStatement("SELECT * FROM runs ORDER BY started_at DESC LIMIT 100");
         ResultSet rows = statement.executeQuery()) {
      while (rows.next()) result.add(readRun(rows));
    }
    return result;
  }

  synchronized Run run(String id) throws SQLException {
    try (PreparedStatement statement = db.prepareStatement("SELECT * FROM runs WHERE id=?")) {
      statement.setString(1, id);
      try (ResultSet rows = statement.executeQuery()) {
        return rows.next() ? readRun(rows) : null;
      }
    }
  }

  synchronized Detail detail(String id) throws SQLException {
    return readDetail(id, false);
  }

  synchronized Detail reportDetail(String id) throws SQLException {
    return readDetail(id, true);
  }

  private Detail readDetail(String id, boolean fullEvents) throws SQLException {
    Run run = run(id);
    if (run == null) return null;
    UsageAudit audit = auditedUsage(run);
    run = audit.run();
    List<Event> events = new ArrayList<>();
    String query = "SELECT seq,at,type,kind,title,detail,payload FROM events WHERE run_id=? "
        + (fullEvents ? "ORDER BY seq" : "ORDER BY seq DESC LIMIT 500");
    try (PreparedStatement statement = db.prepareStatement(query)) {
      statement.setString(1, id);
      try (ResultSet rows = statement.executeQuery()) {
        while (rows.next()) {
          EventLogic.Usage usage = null;
          try { usage = EventLogic.usage(JSON.readTree(rows.getString(7))); }
          catch (Exception ignored) { /* Older captured payloads may be incomplete. */ }
          events.add(new Event(rows.getInt(1), rows.getString(2), rows.getString(3),
              rows.getString(4), rows.getString(5), rows.getString(6), usage));
        }
      }
    }
    if (!fullEvents) Collections.reverse(events);
    List<Artifact> artifacts = new ArrayList<>();
    try (PreparedStatement statement = db.prepareStatement("SELECT file,category,first_seen,bytes,estimated_tokens,sha256 FROM artifacts WHERE run_id=? ORDER BY first_seen")) {
      statement.setString(1, id);
      try (ResultSet rows = statement.executeQuery()) {
        while (rows.next()) artifacts.add(new Artifact(rows.getString(1), rows.getString(2), rows.getString(3), rows.getLong(4), rows.getLong(5), rows.getString(6)));
      }
    }
    List<Sample> samples = new ArrayList<>();
    try (PreparedStatement statement = db.prepareStatement("SELECT at,cpu_percent,memory_bytes FROM samples WHERE run_id=? ORDER BY at DESC LIMIT 240")) {
      statement.setString(1, id);
      try (ResultSet rows = statement.executeQuery()) {
        while (rows.next()) samples.add(new Sample(rows.getString(1), rows.getDouble(2), rows.getLong(3)));
      }
    }
    Collections.reverse(samples);
    return new Detail(run, events, artifacts, samples, logic.sources(run.prompt(), artifacts), audit.auditable());
  }

  synchronized List<Event> flowEvents(String id) throws SQLException {
    List<Event> events = new ArrayList<>();
    try (PreparedStatement statement = db.prepareStatement(
        "SELECT seq,at,type,kind,payload FROM events WHERE run_id=? "
            + "AND (type IN ('turn.started','turn.completed') OR kind='message') ORDER BY seq")) {
      statement.setString(1, id);
      try (ResultSet rows = statement.executeQuery()) {
        while (rows.next()) {
          String type = rows.getString(3);
          EventLogic.Usage usage = null;
          try { usage = EventLogic.usage(JSON.readTree(rows.getString(5))); }
          catch (Exception ignored) { /* Older event payloads may be incomplete. */ }
          events.add(new Event(rows.getInt(1), rows.getString(2), type, rows.getString(4), "", "", usage));
        }
      }
    }
    return List.copyOf(events);
  }

  synchronized String eventPayload(String id, int sequence) throws SQLException {
    try (PreparedStatement statement = db.prepareStatement(
        "SELECT payload FROM events WHERE run_id=? AND seq=?")) {
      statement.setString(1, id);
      statement.setInt(2, sequence);
      try (ResultSet rows = statement.executeQuery()) {
        return rows.next() ? rows.getString(1) : null;
      }
    }
  }
  synchronized TraceEvents traceEvents(String id) throws SQLException {
    List<RawEvent> items = new ArrayList<>();
    int total = 0;
    try (PreparedStatement statement = db.prepareStatement(
        "SELECT seq,at,payload FROM events WHERE run_id=? AND type='item.completed' ORDER BY seq")) {
      statement.setString(1, id);
      try (ResultSet rows = statement.executeQuery()) {
        while (rows.next()) {
          String payload = rows.getString(3);
          try {
            String kind = JSON.readTree(payload).path("item").path("type").asText();
            if (!"command_execution".equals(kind) && !"file_change".equals(kind)) continue;
            total++;
            if (items.size() < 400) items.add(new RawEvent(rows.getInt(1), rows.getString(2), payload));
          } catch (Exception ignored) { /* O JSONL original permanece disponível. */ }
        }
      }
    }
    return new TraceEvents(List.copyOf(items), total);
  }
  synchronized void saveQuotaSnapshots(String id, List<QuotaSnapshot> snapshots) throws SQLException {
    if (run(id) == null) return;
    try (PreparedStatement statement = db.prepareStatement("""
        INSERT OR REPLACE INTO quota_snapshots
        (run_id,phase,limit_id,window_name,window_minutes,resets_at,used_percent,captured_at,plan_type)
        VALUES (?,?,?,?,?,?,?,?,?)""")) {
      for (QuotaSnapshot snapshot : snapshots) {
        statement.setString(1, id);
        statement.setString(2, snapshot.phase());
        statement.setString(3, snapshot.limitId());
        statement.setString(4, snapshot.window());
        statement.setInt(5, snapshot.windowMinutes());
        statement.setLong(6, snapshot.resetsAt());
        statement.setDouble(7, snapshot.usedPercent());
        statement.setString(8, snapshot.capturedAt());
        statement.setString(9, snapshot.planType());
        statement.addBatch();
      }
      statement.executeBatch();
    }
  }

  synchronized List<QuotaSnapshot> quotaSnapshots(String id) throws SQLException {
    List<QuotaSnapshot> result = new ArrayList<>();
    try (PreparedStatement statement = db.prepareStatement("""
        SELECT phase,limit_id,window_name,window_minutes,resets_at,used_percent,captured_at,plan_type
        FROM quota_snapshots WHERE run_id=? ORDER BY limit_id,window_name,phase""")) {
      statement.setString(1, id);
      try (ResultSet rows = statement.executeQuery()) {
        while (rows.next()) result.add(new QuotaSnapshot(rows.getString(1), rows.getString(2),
            rows.getString(3), rows.getInt(4), rows.getLong(5), rows.getDouble(6),
            rows.getString(7), rows.getString(8)));
      }
    }
    return result;
  }
  private record UsageAudit(Run run, boolean auditable) {}

  private UsageAudit auditedUsage(Run stored) throws SQLException {
    long input = 0, cached = 0, output = 0, reasoning = 0, total = 0, missing = 0;
    long results = 0;
    try (PreparedStatement statement = db.prepareStatement(
        "SELECT payload FROM events WHERE run_id=? AND type='turn.completed' ORDER BY seq")) {
      statement.setString(1, stored.id());
      try (ResultSet rows = statement.executeQuery()) {
        while (rows.next()) {
          results++;
          EventLogic.Usage usage = null;
          try { usage = EventLogic.usage(JSON.readTree(rows.getString(1))); }
          catch (Exception ignored) { /* Eventos antigos podem estar incompletos. */ }
          if (usage == null) missing++;
          else {
            input += usage.input();
            cached += usage.cached();
            output += usage.output();
            reasoning += usage.reasoning();
            total += usage.total();
          }
        }
      }
    }
    if (results == 0) return new UsageAudit(stored, false);
    return new UsageAudit(new Run(stored.id(), stored.project(), stored.prompt(), stored.model(), stored.sandbox(),
        stored.provider(), stored.status(), stored.startedAt(), stored.endedAt(), stored.exitCode(),
        stored.error(), input, cached, output, reasoning, total, missing,
        stored.cpuPeak(), stored.memoryPeak()), true);
  }
  synchronized String artifactContent(String id, String file) throws SQLException {
    try (PreparedStatement statement = db.prepareStatement(
        "SELECT content FROM artifact_snapshots WHERE run_id=? AND file=?")) {
      statement.setString(1, id);
      statement.setString(2, file);
      try (ResultSet rows = statement.executeQuery()) {
        return rows.next() ? rows.getString(1) : null;
      }
    }
  }

  synchronized Run begin(Path project, String prompt, String model, String sandbox) throws SQLException {
    return begin(project, prompt, model, sandbox, "codex");
  }

  synchronized Run begin(Path project, String prompt, String model, String sandbox, String provider) throws SQLException {
    String id = UUID.randomUUID().toString();
    try (PreparedStatement statement = db.prepareStatement(
        "INSERT INTO runs (id,project,prompt,model,sandbox,provider,status,started_at) VALUES (?,?,?,?,?,?,'running',?)")) {
      statement.setString(1, id);
      statement.setString(2, project.toAbsolutePath().normalize().toString());
      statement.setString(3, prompt);
      statement.setString(4, model.isBlank() ? null : model);
      statement.setString(5, sandbox);
      statement.setString(6, provider);
      statement.setString(7, Instant.now().toString());
      statement.executeUpdate();
    }
    return run(id);
  }

  synchronized void recordEvent(String id, JsonNode event, String json) throws SQLException, IOException {
    EventLogic.Summary summary = EventLogic.summarize(event);
    String at = Instant.now().toString();
    int seq;
    try (PreparedStatement statement = db.prepareStatement("SELECT COALESCE(MAX(seq),0)+1 FROM events WHERE run_id=?")) {
      statement.setString(1, id);
      try (ResultSet rows = statement.executeQuery()) { rows.next(); seq = rows.getInt(1); }
    }
    try (PreparedStatement statement = db.prepareStatement("INSERT INTO events VALUES (?,?,?,?,?,?,?,?)")) {
      statement.setString(1, id);
      statement.setInt(2, seq);
      statement.setString(3, at);
      statement.setString(4, event.path("type").asText("unknown"));
      statement.setString(5, summary.kind());
      statement.setString(6, summary.title());
      statement.setString(7, summary.detail().substring(0, Math.min(16000, summary.detail().length())));
      statement.setString(8, json);
      statement.executeUpdate();
    }
    Files.writeString(rawFile(id), "{\"at\":\"" + at + "\",\"event\":" + json + "}\n",
        StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
    if ("turn.completed".equals(event.path("type").asText())) {
      EventLogic.Usage usage = EventLogic.usage(event);
      if (usage == null) {
        try (PreparedStatement statement = db.prepareStatement("UPDATE runs SET missing_usage=missing_usage+1 WHERE id=?")) {
          statement.setString(1, id);
          statement.executeUpdate();
        }
      } else {
        try (PreparedStatement statement = db.prepareStatement("""
            UPDATE runs SET input_tokens=input_tokens+?, cached_tokens=cached_tokens+?,
            output_tokens=output_tokens+?, reasoning_tokens=reasoning_tokens+?,
            total_tokens=total_tokens+? WHERE id=?""")) {
          statement.setLong(1, usage.input());
          statement.setLong(2, usage.cached());
          statement.setLong(3, usage.output());
          statement.setLong(4, usage.reasoning());
          statement.setLong(5, usage.total());
          statement.setString(6, id);
          statement.executeUpdate();
        }
      }
    }
  }

  synchronized void observeArtifacts(String id, String text, Path project) {
    for (Path file : EventLogic.pathsFromText(text, project)) {
      try {
        if (!Files.isRegularFile(file) || Files.size(file) > 1048576) continue;
        try (PreparedStatement check = db.prepareStatement("SELECT 1 FROM artifacts WHERE run_id=? AND file=?")) {
          check.setString(1, id);
          check.setString(2, file.toString());
          try (ResultSet rows = check.executeQuery()) { if (rows.next()) continue; }
        }
        byte[] bytes = Files.readAllBytes(file);
        String content = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes)).toString();
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        try (PreparedStatement insert = db.prepareStatement("INSERT OR IGNORE INTO artifacts VALUES (?,?,?,?,?,?,?)")) {
          insert.setString(1, id);
          insert.setString(2, file.toString());
          insert.setString(3, EventLogic.category(file));
          insert.setString(4, Instant.now().toString());
          insert.setLong(5, bytes.length);
          insert.setLong(6, logic.tokens(content));
          insert.setString(7, hash);
          insert.executeUpdate();
        }
        try (PreparedStatement snapshot = db.prepareStatement(
            "INSERT OR IGNORE INTO artifact_snapshots (run_id,file,content) VALUES (?,?,?)")) {
          snapshot.setString(1, id);
          snapshot.setString(2, file.toString());
          snapshot.setString(3, content);
          snapshot.executeUpdate();
        }
      } catch (IOException | SQLException | NoSuchAlgorithmException | RuntimeException ignored) {
        // A referenced file may be unavailable, binary or newly created.
      }
    }
  }

  synchronized void sample(String id, double cpu, long memory) throws SQLException {
    String at = Instant.now().toString();
    try (PreparedStatement insert = db.prepareStatement("INSERT INTO samples VALUES (?,?,?,?)");
         PreparedStatement update = db.prepareStatement("UPDATE runs SET cpu_peak=MAX(cpu_peak,?),memory_peak=MAX(memory_peak,?) WHERE id=?")) {
      insert.setString(1, id);
      insert.setString(2, at);
      insert.setDouble(3, cpu);
      insert.setLong(4, memory);
      insert.executeUpdate();
      update.setDouble(1, cpu);
      update.setLong(2, memory);
      update.setString(3, id);
      update.executeUpdate();
    }
  }

  synchronized void finish(String id, String status, Integer exitCode, String error) throws SQLException {
    try (PreparedStatement statement = db.prepareStatement("UPDATE runs SET status=?,ended_at=?,exit_code=?,error=? WHERE id=?")) {
      statement.setString(1, status);
      statement.setString(2, Instant.now().toString());
      if (exitCode == null) statement.setNull(3, java.sql.Types.INTEGER); else statement.setInt(3, exitCode);
      statement.setString(4, error);
      statement.setString(5, id);
      statement.executeUpdate();
    }
  }

  synchronized void restart(String id) throws SQLException, IOException {
    if (run(id) == null) throw new IllegalArgumentException("Sessão não encontrada.");
    Files.deleteIfExists(rawFile(id));
    db.setAutoCommit(false);
    try {
      clearObservations(id);
      try (PreparedStatement statement = db.prepareStatement("""
          UPDATE runs SET status='running',started_at=?,ended_at=NULL,exit_code=NULL,error=NULL,
          input_tokens=0,cached_tokens=0,output_tokens=0,reasoning_tokens=0,total_tokens=0,
          missing_usage=0,cpu_peak=0,memory_peak=0 WHERE id=?""")) {
        statement.setString(1, Instant.now().toString());
        statement.setString(2, id);
        statement.executeUpdate();
      }
      db.commit();
    } catch (SQLException error) {
      db.rollback();
      throw error;
    } finally {
      db.setAutoCommit(true);
    }
  }

  synchronized void delete(String id) throws SQLException, IOException {
    if (run(id) == null) throw new IllegalArgumentException("Sessão não encontrada.");
    Files.deleteIfExists(rawFile(id));
    db.setAutoCommit(false);
    try {
      clearObservations(id);
      try (PreparedStatement statement = db.prepareStatement("DELETE FROM runs WHERE id=?")) {
        statement.setString(1, id);
        statement.executeUpdate();
      }
      db.commit();
    } catch (SQLException error) {
      db.rollback();
      throw error;
    } finally {
      db.setAutoCommit(true);
    }
  }

  private void clearObservations(String id) throws SQLException {
    for (String table : List.of("events", "artifacts", "artifact_snapshots", "samples", "quota_snapshots")) {
      try (PreparedStatement statement = db.prepareStatement("DELETE FROM " + table + " WHERE run_id=?")) {
        statement.setString(1, id);
        statement.executeUpdate();
      }
    }
  }

  private static Run readRun(ResultSet rows) throws SQLException {
    Object exit = rows.getObject("exit_code");
    return new Run(rows.getString("id"), rows.getString("project"), rows.getString("prompt"),
        rows.getString("model"), rows.getString("sandbox"), rows.getString("provider"), rows.getString("status"),
        rows.getString("started_at"), rows.getString("ended_at"), exit == null ? null : ((Number) exit).intValue(),
        rows.getString("error"), rows.getLong("input_tokens"), rows.getLong("cached_tokens"),
        rows.getLong("output_tokens"), rows.getLong("reasoning_tokens"), rows.getLong("total_tokens"),
        rows.getLong("missing_usage"), rows.getDouble("cpu_peak"), rows.getLong("memory_peak"));
  }

  @Override public synchronized void close() throws SQLException {
    db.close();
  }
}

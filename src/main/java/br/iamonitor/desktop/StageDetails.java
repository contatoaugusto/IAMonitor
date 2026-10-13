package br.iamonitor.desktop;

import java.nio.file.Path;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.List;

final class StageDetails {
  private static final NumberFormat NUM =
      NumberFormat.getIntegerInstance(Locale.forLanguageTag("pt-BR"));

  private StageDetails() {}

  static String title(int stage) {
    return switch (stage) {
      case 1 -> "Pedido";
      case 2 -> "CLI local";
      case 3 -> "Contexto";
      case 4 -> "Envio ao modelo";
      case 5 -> "Modelo remoto";
      case 6 -> "Retorno";
      case 7 -> "Ações locais";
      case 8 -> "Conclusão";
      case 9 -> "Cache";
      default -> "Etapa";
    };
  }

  static String describe(Store store, Store.Detail detail, int stage) {
    if (detail == null) return "Sem sessão selecionada.";
    Store.Run run = detail.run();
    return switch (stage) {
      case 1 -> present(run.prompt());
      case 2 -> cli(detail);
      case 3 -> artifacts(store, detail);
      case 4, 5, 6, 8, 9 -> usage(store, detail, stage);
      case 7 -> events(detail, true);
      default -> "Sem registros.";
    };
  }

  private static String cli(Store.Detail detail) {
    StringBuilder text = new StringBuilder();
    text.append("CLI: ").append(Provider.name(detail.run().provider())).append('\n')
        .append("Pasta: ").append(detail.run().project()).append("\n\n");
    text.append(events(detail, false));
    return text.toString().strip();
  }

  private static String artifacts(Store store, Store.Detail detail) {
    StringBuilder text = new StringBuilder();
    Path root = Path.of(detail.run().project()).toAbsolutePath().normalize();
    int count = 0;
    for (Store.Artifact artifact : detail.artifacts()) {
      String source = EventLogic.sourceKey(artifact.category());
      if (!"spec".equals(source) && !"skill".equals(source)
          && !"agent".equals(source) && !"hook".equals(source)) continue;
      count++;
      Path file;
      try { file = Path.of(artifact.file()).toAbsolutePath().normalize(); }
      catch (RuntimeException error) {
        text.append(source.toUpperCase(Locale.ROOT)).append(" · ")
            .append(artifact.file()).append("\n\n");
        continue;
      }
      String relative = file.startsWith(root)
          ? root.relativize(file).toString() : artifact.file();
      text.append(source.toUpperCase(Locale.ROOT)).append(" · ")
          .append(relative).append('\n')
          .append("Caminho: ").append(file).append('\n');
      String observed = null;
      try { observed = store.artifactContent(detail.run().id(), artifact.file()); }
      catch (SQLException ignored) { /* Bancos antigos podem não ter snapshot. */ }
      if (observed == null)
        observed = SourceDetails.contentAtObservation(root, file, artifact.sha256());
      if (!unavailable(observed)) text.append("\n").append(observed).append('\n');
      text.append('\n');
    }
    return count == 0 ? "Sem arquivos registrados." : text.toString().strip();
  }

  private static String usage(Store store, Store.Detail detail, int stage) {
    Store.Run run = detail.run();
    if (!detail.usageAuditable()) return "Sem contagem verificável registrada.";
    boolean reported = run.totalTokens() > 0 || run.inputTokens() + run.outputTokens() > 0
        || detail.events().stream().anyMatch(event -> event.usage() != null);
    if (!reported) return stage == 6 ? messages(detail) : "Sem contagem registrada.";
    TokenAccounting.Assessment count = TokenAccounting.assess(run, true, true);
    if ("—".equals(count.total())) return "Sem contagem verificável registrada.";
    StringBuilder text = new StringBuilder();
    switch (stage) {
      case 4 -> text.append("Entrada: ").append(num(run.inputTokens()))
          .append(" tokens\nCache na entrada: ").append(num(run.cachedTokens())).append(" tokens");
      case 5 -> text.append("Total: ").append(count.total())
          .append(" tokens\nEntrada: ").append(num(run.inputTokens()))
          .append("\nSaída: ").append(num(run.outputTokens()));
      case 6 -> text.append("Saída: ").append(num(run.outputTokens()))
          .append(" tokens\nRaciocínio na saída: ")
          .append(num(run.reasoningTokens())).append(" tokens");
      case 8 -> text.append("Total: ").append(count.total())
          .append(" tokens\nEntrada: ").append(num(run.inputTokens()))
          .append("\nSaída: ").append(num(run.outputTokens()))
          .append("\nStatus: ").append(count.status());
      case 9 -> {
        if (run.cachedTokens() <= 0) return "Sem cache registrado.";
        text.append("Cache na entrada: ").append(num(run.cachedTokens())).append(" tokens");
      }
      default -> {}
    }
    List<Store.Event> flow;
    try { flow = store.flowEvents(run.id()); }
    catch (SQLException error) { flow = detail.events(); }
    int unit = 0, listed = 0;
    for (Store.Event event : flow) {
      if (!"turn.completed".equals(event.type())) continue;
      unit++;
      EventLogic.Usage part = event.usage();
      if (stage == 9 && (part == null || part.cached() <= 0)) continue;
      if (listed++ == 0) text.append("\n\nUnidades de uso reportadas:\n");
      text.append(unit).append(" · ").append(event.at()).append(" · ");
      if (part == null) {
        text.append("sem contagem registrada\n");
        continue;
      }
      switch (stage) {
        case 4 -> text.append("Entrada ").append(num(part.input()))
            .append(" · Cache ").append(num(part.cached()));
        case 5, 8 -> text.append("Entrada ").append(num(part.input()))
            .append(" · Saída ").append(num(part.output()))
            .append(" · Total ").append(num(part.total()));
        case 6 -> text.append("Saída ").append(num(part.output()))
            .append(" · Raciocínio ").append(num(part.reasoning()));
        case 9 -> text.append("Cache ").append(num(part.cached()));
        default -> {}
      }
      text.append('\n');
    }    if (stage == 6 || stage == 8) {
      String visible = messages(detail);
      if (!"Sem mensagens registradas.".equals(visible))
        text.append("\n").append(visible);
    }
    return text.toString().strip();
  }

  private static String events(Store.Detail detail, boolean localOnly) {
    StringBuilder text = new StringBuilder();
    for (Store.Event event : detail.events()) {
      if (localOnly && !"local".equals(event.kind())) continue;
      if (!localOnly && !"thread.started".equals(event.type()) && !"turn.started".equals(event.type())) continue;
      text.append(event.at()).append(" · ").append(event.title()).append('\n');
      if (event.detail() != null && !event.detail().isBlank())
        text.append(event.detail()).append('\n');
      text.append('\n');
    }
    return text.isEmpty() ? "Sem eventos registrados." : text.toString().strip();
  }

  private static String messages(Store.Detail detail) {
    StringBuilder text = new StringBuilder();
    for (Store.Event event : detail.events()) {
      if (!"message".equals(event.kind())) continue;
      if (event.detail() == null || event.detail().isBlank()) continue;
      text.append(event.at()).append("\n").append(event.detail()).append("\n\n");
    }
    return text.isEmpty() ? "Sem mensagens registradas." : text.toString().strip();
  }

  private static boolean unavailable(String content) {
    return content.startsWith("[Arquivo fora da pasta")
        || content.startsWith("[Arquivo não existe")
        || content.startsWith("[Arquivo atual maior")
        || content.startsWith("[O arquivo mudou")
        || content.startsWith("[Não foi possível ler");
  }
  private static String present(String value) {
    return value == null || value.isBlank() ? "Sem pedido registrado." : value;
  }

  private static String num(long value) { return NUM.format(value); }
}

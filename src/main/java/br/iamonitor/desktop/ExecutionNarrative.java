package br.iamonitor.desktop;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

final class ExecutionNarrative {
  record State(String phase, String headline, String detail, String meta, boolean working) {}

  static State describe(Store.Run run, List<Store.Event> events, int files, boolean active) {
    String elapsed = elapsed(run.startedAt(), run.endedAt());
    String meta = elapsed + "  ·  " + events.size() + " eventos recentes  ·  " + files + " arquivos identificados";
    if ("completed".equals(run.status()))
      return new State("CONCLUÍDA", "Tarefa concluída", "A CLI encerrou a execução. Os tokens reportados estão no painel acima.", meta, false);
    if ("failed".equals(run.status()))
      return new State("FALHOU", "Execução não concluída", run.error() == null || run.error().isBlank()
          ? "A CLI encerrou com erro." : run.error(), meta, false);
    if ("interrupted".equals(run.status()))
      return new State("INTERROMPIDA", "Execução interrompida", "A sessão foi interrompida antes da conclusão.", meta, false);
    if (!active)
      return new State("AGUARDANDO", "Aguardando processo da IA", "Nenhum processo ativo foi identificado nesta sessão.", meta, false);
    if (events.isEmpty())
      return new State("INICIANDO", "Conectando à CLI", "O pedido foi enviado. Aguardando o primeiro evento da IA...", meta, true);
    Store.Event latest = events.getLast();
    try {
      long silence = Duration.between(Instant.parse(latest.at()), Instant.now()).toSeconds();
      if (silence >= 5) {
        String previous = latest.title() + (latest.detail() == null || latest.detail().isBlank()
            ? "" : " · " + latest.detail().replace('\n', ' '));
        return new State("AGUARDANDO EVENTO", "A IA continua em execução",
            "Sem nova atualização há " + silence + " s. Último evento: " + previous, meta, true);
      }
    } catch (RuntimeException ignored) { /* Histórico antigo pode não ter horário ISO. */ }
    if ("turn.completed".equals(latest.type()))
      return new State("FINALIZANDO", "Resposta recebida do modelo", "Aguardando o encerramento da CLI para confirmar a conclusão.", meta, true);
    String phase = switch (latest.kind()) {
      case "local" -> "OPERAÇÃO LOCAL";
      case "model", "message" -> "MODELO DE IA";
      case "error" -> "ERRO OBSERVADO";
      default -> "EVENTO RECEBIDO";
    };
    String detail = latest.detail() == null || latest.detail().isBlank()
        ? "Aguardando a próxima operação..." : latest.detail().replace('\n', ' ');
    return new State(phase, latest.title(), detail, meta, true);
  }

  private static String elapsed(String startedAt, String endedAt) {
    try {
      Instant end = endedAt == null ? Instant.now() : Instant.parse(endedAt);
      long seconds = Math.max(0, Duration.between(Instant.parse(startedAt), end).toSeconds());
      return String.format("%02d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60);
    } catch (RuntimeException error) { return "00:00:00"; }
  }
}

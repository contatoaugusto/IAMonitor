package br.iamonitor.desktop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class DataTrace {
  private static final ObjectMapper JSON = new ObjectMapper();
  private static final int OUTPUT_PREVIEW = 8000;

  record Evidence(int seq, String at, String kind, String command, String output,
      int outputLength, boolean previewTruncated, List<String> paths) {}
  record Snapshot(Store.Run run, List<Evidence> evidence, List<Store.Artifact> artifacts,
      int totalEvidence, boolean limited) {
    long commandsWithOutput() {
      return evidence.stream().filter(item -> "command".equals(item.kind())
          && item.outputLength() > 0).count();
    }
  }

  private DataTrace() {}

  static Snapshot from(Store.Run run, Store.TraceEvents events, List<Store.Artifact> artifacts) {
    List<Evidence> evidence = new ArrayList<>();
    Path project = Path.of(run.project());
    for (Store.RawEvent raw : events.items()) {
      try {
        JsonNode item = JSON.readTree(raw.payload()).path("item");
        if ("command_execution".equals(item.path("type").asText())) {
          String command = item.path("command").asText("");
          String output = item.path("aggregated_output").asText("");
          int length = output.length();
          List<String> paths = EventLogic.pathsFromText(command, project).stream()
              .map(Path::toString).toList();
          evidence.add(new Evidence(raw.seq(), raw.at(), "command", command,
              output.substring(0, Math.min(length, OUTPUT_PREVIEW)), length,
              length > OUTPUT_PREVIEW, paths));
        } else if ("file_change".equals(item.path("type").asText())) {
          List<String> paths = new ArrayList<>();
          for (JsonNode change : item.path("changes")) {
            String path = change.path("path").asText("");
            if (!path.isBlank()) paths.add(path);
          }
          evidence.add(new Evidence(raw.seq(), raw.at(), "change", "", "", 0, false,
              List.copyOf(paths)));
        }
      } catch (Exception ignored) { /* Evento original continua disponível no JSONL. */ }
    }
    return new Snapshot(run, List.copyOf(evidence), List.copyOf(artifacts),
        events.total(), events.total() > evidence.size());
  }

  static String describe(Snapshot snapshot) {
    StringBuilder text = new StringBuilder();
    text.append("TRILHA DE DADOS DA SESSÃO\n\n");
    text.append("O que é comprovado aqui\n");
    text.append("• O pedido abaixo foi entregue pelo IAMonitor à CLI local.\n");
    text.append("• Os comandos, saídas e mudanças abaixo foram emitidos no fluxo estruturado da CLI.\n");
    text.append("• Os arquivos do catálogo foram identificados localmente; isso não prova leitura nem envio.\n\n");
    text.append("ENVIO REMOTO EXATO: INDISPONÍVEL\n");
    text.append("O IAMonitor não intercepta as requisições HTTPS da CLI. A CLI pode selecionar, resumir, ");
    text.append("truncar ou combinar contexto antes de falar com o modelo. Portanto, estes registros ");
    text.append("não revelam o corpo exato enviado à OpenAI ou a outro provedor, nem quantos tokens ");
    text.append("vieram de cada arquivo. O total da CLI mede a operação, não atribui parcelas a arquivos.\n\n");
    text.append("PEDIDO ENTREGUE À CLI\n").append(snapshot.run().prompt()).append("\n\n");
    text.append("EVENTOS LOCAIS ESTRUTURADOS\n");
    text.append(snapshot.totalEvidence()).append(" comandos ou mudanças registrados");
    if (snapshot.limited()) text.append("; exibindo os primeiros ").append(snapshot.evidence().size())
        .append(". Consulte o JSONL para o histórico completo");
    text.append(".\n");
    if (snapshot.evidence().isEmpty()) text.append("Nenhum comando ou mudança estruturada disponível.\n");
    for (Evidence item : snapshot.evidence()) {
      text.append("\n#").append(item.seq()).append("  ").append(item.at()).append("  ");
      if ("command".equals(item.kind())) {
        text.append("COMANDO LOCAL\n").append(item.command()).append("\n");
        if (!item.paths().isEmpty()) {
          text.append("Caminhos citados no comando (leitura não comprovada):\n");
          for (String path : item.paths()) text.append("  • ").append(path).append("\n");
        }
        text.append("Saída registrada: ").append(item.outputLength()).append(" caracteres. ");
        text.append("É evidência local e possível contexto; não é corpo HTTPS confirmado.\n");
        if (item.outputLength() > 0) text.append(item.output()).append("\n");
        if (item.previewTruncated()) text.append("[Prévia limitada; confira o JSONL original.]\n");
      } else {
        text.append("MUDANÇA DE ARQUIVO LOCAL\n");
        for (String path : item.paths()) text.append("  • ").append(path).append("\n");
        text.append("Uma gravação local não comprova envio remoto.\n");
      }
    }
    text.append("\nARQUIVOS IDENTIFICADOS LOCALMENTE\n");
    if (snapshot.artifacts().isEmpty()) text.append("Nenhum arquivo identificado.\n");
    for (Store.Artifact artifact : snapshot.artifacts())
      text.append("• ").append(artifact.category()).append("  |  ").append(artifact.file())
          .append("  |  aprox. ").append(artifact.estimatedTokens())
          .append(" tokens locais, sem atribuição ao envio\n");
    text.append("\nRevise esta tela, o JSONL e o PDF antes de compartilhar uma sessão: ");
    text.append("comandos e saídas podem conter código ou dados privados.\n");
    return text.toString();
  }
}
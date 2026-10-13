package br.iamonitor.desktop;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.NumberFormat;
import java.util.HexFormat;
import java.util.Locale;

final class SourceDetails {
  private SourceDetails() {}

  static String describeCategory(Store.Detail detail, String category) {
    NumberFormat numbers = NumberFormat.getIntegerInstance(Locale.forLanguageTag("pt-BR"));
    StringBuilder text = new StringBuilder(category.toUpperCase(Locale.ROOT)).append("\n\n");
    text.append("Arquivos identificados nesta categoria. Os tokens abaixo são estimativas locais do texto dos arquivos; não são uma parcela medida pela CLI nem comprovam envio integral ao modelo.\n\n");
    Path root = Path.of(detail.run().project()).toAbsolutePath().normalize();
    int count = 0;
    long total = 0;
    for (Store.Artifact artifact : detail.artifacts()) {
      String actualCategory = artifact.category() == null || artifact.category().isBlank()
          ? "Arquivo" : artifact.category();
      if (!category.equals(actualCategory)) continue;
      count++;
      long estimate = Math.max(0, artifact.estimatedTokens());
      total += estimate;
      String path = artifact.file();
      try {
        Path file = Path.of(path).toAbsolutePath().normalize();
        if (file.startsWith(root)) path = root.relativize(file).toString();
      } catch (RuntimeException ignored) { /* Preserve the observed path. */ }
      text.append(count).append(". ").append(path).append('\n');
      text.append("   Estimativa local: ").append(numbers.format(estimate)).append(" tokens\n\n");
    }
    if (count == 0) text.append("Nenhum arquivo desta categoria foi identificado. Isso não indica consumo zero de contexto.\n");
    else text.append("Total: ").append(count).append(count == 1 ? " arquivo, " : " arquivos, ")
        .append(numbers.format(total)).append(" tokens estimados localmente.\n");
    return text.toString();
  }

  static String describe(Store store, Store.Detail detail, String source) {
    if ("prompt".equals(source)) {
      EventLogic.Source estimate = detail.inputSources().get("prompt");
      return "PROMPT SIMPLES\n\n"
          + "Texto registrado no pedido desta sessão:\n\n" + detail.run().prompt()
          + "\n\nTokens estimados localmente: " + estimate.estimatedTokens()
          + "\n\nA contagem é uma estimativa do texto do pedido. O uso reportado pela CLI pode incluir outros contextos.";
    }
    if ("cache".equals(source)) return cacheDetails(detail);
    StringBuilder text = new StringBuilder();
    text.append(source.toUpperCase()).append("\n\n");
    text.append("Arquivos identificados nesta origem. A identificação não prova que o conteúdo inteiro foi enviado ao modelo.\n\n");
    Path root = Path.of(detail.run().project()).toAbsolutePath().normalize();
    int count = 0;
    long estimated = 0;
    for (Store.Artifact artifact : detail.artifacts()) {
      if (!source.equals(EventLogic.sourceKey(artifact.category()))) continue;
      count++;
      estimated += artifact.estimatedTokens();
      Path file;
      try { file = Path.of(artifact.file()).toAbsolutePath().normalize(); }
      catch (RuntimeException error) {
        text.append("Caminho inválido: ").append(artifact.file()).append("\n\n");
        continue;
      }
      String relative = file.startsWith(root) ? root.relativize(file).toString() : artifact.file();
      String name = "skill".equals(source) && file.getFileName().toString().equalsIgnoreCase("SKILL.md")
          ? file.getParent().getFileName().toString() : file.getFileName().toString();
      text.append(count).append(". ").append(name).append('\n');
      text.append("No projeto: ").append(relative).append('\n');
      text.append("Caminho completo: ").append(file).append('\n');
      text.append("Identificado em: ").append(artifact.firstSeen()).append('\n');
      text.append("Tokens estimados: ").append(artifact.estimatedTokens()).append('\n');
      text.append("SHA-256 observado: ").append(artifact.sha256()).append('\n');
      String observed = null;
      try { observed = store.artifactContent(detail.run().id(), artifact.file()); }
      catch (java.sql.SQLException ignored) { /* Legacy databases can use a matching local file. */ }
      text.append("Conteúdo identificado na coleta:\n")
          .append(observed == null ? contentAtObservation(root, file, artifact.sha256()) : observed)
          .append("\n\n");
    }
    if (count == 0) text.append("Nenhum arquivo desta origem foi identificado no pedido ou nos eventos capturados.\n");
    else text.append("Total: ").append(count).append(" arquivo(s), ")
        .append(estimated).append(" tokens estimados localmente.\n");
    return text.toString();
  }

  private static String cacheDetails(Store.Detail detail) {
    StringBuilder text = new StringBuilder();
    text.append("CACHE DE ENTRADA REPORTADO\n\n");
    text.append("Total da sessão: ").append(detail.run().cachedTokens()).append(" tokens em cache.\n");
    text.append("Cache é contexto que o modelo de IA reaproveitou. Esses tokens fazem parte do conteúdo recebido pelo modelo (entrada) reportado por " + Provider.name(detail.run().provider()) + ". ");
    text.append("O fluxo não informa o conteúdo armazenado nem comprova quantos bytes foram transmitidos pela rede.\n\n");
    int turns = 0;
    for (Store.Event event : detail.events()) {
      EventLogic.Usage usage = event.usage();
      if (usage == null || usage.cached() <= 0) continue;
      turns++;
      text.append("Turno ").append(turns).append(" · ").append(event.at()).append('\n');
      text.append("IA recebeu (entrada): ").append(usage.input())
          .append(" · Em cache: ").append(usage.cached())
          .append(" · Modelo gerou (saída): ").append(usage.output())
          .append(" · Total: ").append(usage.total()).append("\n\n");
    }
    if (turns == 0) text.append("A contagem total veio do registro da sessão; não há turnos com cache entre os eventos recentes disponíveis.\n");
    return text.toString();
  }

  static String contentAtObservation(Path root, Path file, String expectedHash) {
    if (!file.startsWith(root)) return "[Arquivo fora da pasta do projeto; conteúdo indisponível.]";
    try {
      if (!Files.isRegularFile(file)) return "[Arquivo não existe mais no projeto.]";
      if (Files.size(file) > 1048576) return "[Arquivo atual maior que 1 MB; conteúdo não exibido.]";
      byte[] bytes = Files.readAllBytes(file);
      String currentHash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
      if (!currentHash.equalsIgnoreCase(expectedHash))
        return "[O arquivo mudou desde a identificação. O conteúdo original não foi preservado.]";
      return new String(bytes, StandardCharsets.UTF_8);
    } catch (IOException | NoSuchAlgorithmException | RuntimeException error) {
      return "[Não foi possível ler o conteúdo atual: " + error.getMessage() + "]";
    }
  }
}

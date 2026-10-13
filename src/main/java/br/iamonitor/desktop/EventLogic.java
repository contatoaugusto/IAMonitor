package br.iamonitor.desktop;

import com.fasterxml.jackson.databind.JsonNode;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingType;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class EventLogic {
  record Usage(long input, long cached, long output, long reasoning, long total) {}
  record Summary(String kind, String title, String detail) {}
  record Source(Long estimatedTokens, int fileCount) {}

  static final List<String> ARTIFACT_CATEGORIES = List.of(
      "Skill", "Agent", "Command", "Spec", "OpenSpec", "Hook", "Instruções", "Arquivo");

  private static final String EXT = "(?:md|txt|json|ya?ml|toml|tsx?|jsx?|mjs|cjs|css|html|py|ps1|sql)";
  private static final Pattern FILE_IN_QUOTES = Pattern.compile("[\"'\\x60]([^\"'\\x60\\r\\n]+\\." + EXT + ")[\"'\\x60]", Pattern.CASE_INSENSITIVE);
  private static final Pattern BARE_FILE = Pattern.compile("(?:^|\\s)([\\w./\\\\:-]+\\." + EXT + ")(?=\\s|$)", Pattern.CASE_INSENSITIVE);
  private final Encoding encoding = Encodings.newDefaultEncodingRegistry().getEncoding(EncodingType.O200K_BASE);

  int tokens(String text) {
    return encoding.encode(text).size();
  }

  static Usage usage(JsonNode event) {
    if (!"turn.completed".equals(event.path("type").asText())) return null;
    JsonNode value = event.path("usage");
    if (!value.isObject() || !validCount(value.path("input_tokens")) || !validCount(value.path("output_tokens"))) return null;
    JsonNode cachedNode = value.hasNonNull("cached_input_tokens")
        ? value.path("cached_input_tokens") : value.path("input_tokens_details").path("cached_tokens");
    JsonNode reasoningNode = value.hasNonNull("reasoning_tokens") ? value.path("reasoning_tokens")
        : value.hasNonNull("reasoning_output_tokens") ? value.path("reasoning_output_tokens")
        : value.path("output_tokens_details").path("reasoning_tokens");
    JsonNode totalNode = value.path("total_tokens");
    if (cachedNode.isValueNode() && !validCount(cachedNode)
        || reasoningNode.isValueNode() && !validCount(reasoningNode)
        || totalNode.isValueNode() && !validCount(totalNode)) return null;
    long input = number(value.path("input_tokens"));
    long output = number(value.path("output_tokens"));
    long cached = number(cachedNode);
    long reasoning = number(reasoningNode);
    long total = totalNode.isValueNode() ? number(totalNode) : input + output;
    return new Usage(input, cached, output, reasoning, total);
  }

  private static boolean validCount(JsonNode node) {
    if (node.isIntegralNumber()) return node.canConvertToLong() && node.asLong() >= 0;
    if (node.isTextual()) {
      try { return Long.parseLong(node.asText()) >= 0; } catch (NumberFormatException ignored) { return false; }
    }
    return false;
  }

  private static long number(JsonNode node) {
    return validCount(node) ? node.asLong() : 0;
  }
  static Summary summarize(JsonNode event) {
    String type = event.path("type").asText("unknown");
    JsonNode item = event.path("item");
    String itemType = item.path("type").asText();
    return switch (type) {
      case "turn.completed" -> new Summary("model", "Turno concluído", "Uso de tokens atualizado");
      case "turn.started" -> new Summary("model", "Turno iniciado", "");
      case "thread.started" -> new Summary("model", "Sessão iniciada", event.path("thread_id").asText());
      case "error" -> new Summary("error", "Erro", event.path("message").asText());
      default -> {
        if (type.startsWith("provider.")) {
          String providerType = event.path("provider_type").asText();
          String kind = providerType.contains("tool") ? "local" : providerType.contains("error") ? "error"
              : providerType.contains("message") || providerType.equals("assistant") ? "message" : "other";
          yield new Summary(kind, event.path("summary").asText(type), event.path("detail").asText());
        }
        String phase = switch (type) {
          case "item.started" -> "iniciado";
          case "item.completed" -> "concluído";
          case "item.updated" -> "atualizado";
          default -> "observado";
        };
        if ("command_execution".equals(itemType))
          yield new Summary("local", "Comando local " + phase, item.path("command").asText());
        if ("file_change".equals(itemType)) {
          List<String> files = new ArrayList<>();
          item.path("changes").forEach(change -> files.add(change.path("path").asText()));
          yield new Summary("local", "Arquivo " + phase, String.join(", ", files));
        }
        if ("web_search".equals(itemType))
          yield new Summary("local", "Busca web " + phase, item.path("query").asText());
        if ("mcp_tool_call".equals(itemType))
          yield new Summary("local", "Ferramenta " + phase,
              item.path("server").asText() + " · " + item.path("tool").asText());
        if ("agent_message".equals(itemType))
          yield new Summary("message", "Mensagem do Codex", item.path("text").asText());
        if ("reasoning".equals(itemType))
          yield new Summary("model", "Raciocínio " + phase, item.path("text").asText());
        yield new Summary("other", type, itemType);
      }
    };
  }

  static String category(Path file) {
    String name = file.getFileName().toString().toLowerCase();
    String normalized = file.toString().replace('\\', '/').toLowerCase();
    if (name.equals("agents.md") || name.endsWith(".agent.md")) return "Agent";
    if (name.endsWith(".command.md") || normalized.contains("/commands/")) return "Command";
    if (normalized.contains("/hooks/") || name.matches(".*(?:^|[-_.])hook(?:[-_.]|$).*")) return "Hook";
    if (name.equals("skill.md") || normalized.contains("/skills/")) return "Skill";
    if (normalized.contains("/openspec/")) return "OpenSpec";
    if (name.matches("spec[^/]*\\.md") || normalized.contains("/specs/")) return "Spec";
    return "Arquivo";
  }

  static List<Path> pathsFromText(String text, Path project) {
    if (text == null) return List.of();
    Path root = project.toAbsolutePath().normalize();
    Set<Path> paths = new LinkedHashSet<>();
    for (Pattern pattern : List.of(FILE_IN_QUOTES, BARE_FILE)) {
      Matcher matcher = pattern.matcher(text);
      while (matcher.find()) {
        try {
          Path candidate = root.resolve(matcher.group(1)).toAbsolutePath().normalize();
          if (!candidate.equals(root) && candidate.startsWith(root)) paths.add(candidate);
        } catch (RuntimeException ignored) { /* Text may contain an invalid path. */ }
      }
    }
    return List.copyOf(paths);
  }

  static String sourceKey(String category) {
    return switch (category) {
      case "Spec", "OpenSpec" -> "spec";
      case "Skill" -> "skill";
      case "Agent", "Instruções" -> "agent";
      case "Hook" -> "hook";
      case "Command" -> "command";
      default -> null;
    };
  }

  Map<String, Source> sources(String prompt, List<Store.Artifact> artifacts) {
    Map<String, Source> result = new LinkedHashMap<>();
    result.put("prompt", new Source((long) tokens(prompt), 0));
    for (String key : List.of("spec", "skill", "agent", "hook", "command"))
      result.put(key, new Source(null, 0));
    for (Store.Artifact artifact : artifacts) {
      String key = sourceKey(artifact.category());
      if (key == null) continue;
      Source old = result.get(key);
      result.put(key, new Source((old.estimatedTokens() == null ? 0 : old.estimatedTokens()) + artifact.estimatedTokens(), old.fileCount() + 1));
    }
    return result;
  }
}

package br.iamonitor.desktop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class Provider {
  private Provider() {}
  static final List<String> IDS = List.of("codex", "claude", "gemini", "cursor", "copilot");
  static final Map<String, String> NAMES = Map.of(
      "codex", "OpenAI Codex", "claude", "Claude Code", "gemini", "Gemini CLI",
      "cursor", "Cursor Agent", "copilot", "GitHub Copilot CLI");

  static String name(String id) { return NAMES.getOrDefault(id, id); }
  static String explanation(String id) {
    String preparation = switch (id) {
      case "codex" -> "No PowerShell, abra 'codex' e conclua o login com a conta desejada.";
      case "claude" -> "No PowerShell, abra 'claude' e conclua o login com a conta desejada.";
      case "gemini" -> "No PowerShell, abra 'gemini' e escolha 'Sign in with Google' com a conta desejada.";
      case "cursor" -> "No PowerShell, use 'agent login' com a conta desejada.";
      case "copilot" -> "No PowerShell, use 'copilot login' com a conta GitHub desejada.";
      default -> "Confira o login da CLI escolhida.";
    };
    String measurement = switch (id) {
      case "codex" -> "Tokens vêm dos turnos concluídos do Codex.";
      case "claude" -> "Tokens vêm do resultado final do Claude Code.";
      case "gemini" -> "Tokens vêm das estatísticas finais do Gemini CLI. Login Google e chave de API podem ter cotas/cobranças diferentes.";
      case "cursor" -> "O fluxo documentado do Cursor mostra operações, mas não contagens de tokens.";
      case "copilot" -> "O fluxo JSON do Copilot não garante tokens nem créditos da assinatura.";
      default -> "";
    };
    return "PREPARAÇÃO  Instale a CLI de " + name(id) + ", deixe seu comando no PATH e autentique-a uma vez na mesma conta do Windows. "
        + preparation + " Se já estiver autenticada, não precisa repetir.\n\n"
        + "EXECUÇÃO  Não precisa entrar na pasta pelo terminal nem iniciar a IA manualmente. "
        + "Ao clicar em 'Executar e acompanhar', o IAMonitor inicia uma nova execução da CLI "
        + "dentro da pasta escolhida e envia o pedido escrito abaixo.\n\n"
        + "ACESSO  O processo roda na sua conta do Windows. A CLI consulta o login salvo por ela ou variáveis de ambiente herdadas. "
        + "O IAMonitor não lê nem copia credenciais: recebe os eventos que essa execução devolve "
        + "e observa arquivos locais identificados. Não se conecta a sessões já abertas "
        + "no terminal/editor nem intercepta o tráfego com o servidor.\n\n"
        + "MEDIÇÃO  " + measurement + " O total reportado não equivale necessariamente "
        + "ao consumo ou cobrança da assinatura.";
  }
  static String executable(String id) {
    String override = System.getenv("IAMONITOR_" + id.toUpperCase(Locale.ROOT) + "_BIN");
    if (override != null && !override.isBlank()) return override;
    return switch (id) {
      case "codex" -> "codex";
      case "claude" -> "claude";
      case "gemini" -> "gemini";
      case "cursor" -> "agent";
      case "copilot" -> "copilot";
      default -> throw new IllegalArgumentException("Ferramenta desconhecida.");
    };
  }

  static boolean shellWrapper(String id) {
    return System.getProperty("os.name").toLowerCase().contains("win")
        && List.of("gemini", "cursor", "copilot").contains(id)
        && !executable(id).toLowerCase(Locale.ROOT).endsWith(".js");
  }

  private static List<String> powershellCommand(String id) {
    String script = switch (id) {
      case "gemini" -> "$a=@('-p',$request,'--output-format','stream-json'); "
          + "if($env:IAMONITOR_MODEL){$a+=@('--model',$env:IAMONITOR_MODEL)}; "
          + "if($env:IAMONITOR_SANDBOX -eq 'read-only'){$a+=@('--approval-mode','plan')} else{$a+=@('--approval-mode','auto_edit')}; "
          + "& $env:IAMONITOR_EXEC @a; exit $LASTEXITCODE";
      case "cursor" -> "$a=@('-p','--output-format','stream-json'); "
          + "if($env:IAMONITOR_MODEL){$a+=@('--model',$env:IAMONITOR_MODEL)}; "
          + "if($env:IAMONITOR_SANDBOX -eq 'read-only'){$a+=@('--mode','ask')}; "
          + "$a+=@($request); & $env:IAMONITOR_EXEC @a; exit $LASTEXITCODE";
      case "copilot" -> "$a=@('-p',$request,'--output-format','json','--no-ask-user'); "
          + "if($env:IAMONITOR_MODEL){$a+=@('--model',$env:IAMONITOR_MODEL)}; "
          + "if($env:IAMONITOR_SANDBOX -eq 'read-only'){$a+=@('--allow-tool','read','--deny-tool','write,shell')} else{$a+=@('--allow-tool','read,write,shell')}; "
          + "& $env:IAMONITOR_EXEC @a; exit $LASTEXITCODE";
      default -> throw new IllegalArgumentException("Ferramenta desconhecida.");
    };
    script = "$request=[Console]::In.ReadToEnd(); "
        + "if(-not (Get-Command $env:IAMONITOR_EXEC -ErrorAction SilentlyContinue)){"
        + "Write-Error 'CLI não instalada ou não encontrada no PATH'; exit 127}; " + script;
    return new ArrayList<>(List.of("powershell.exe", "-NoProfile", "-NonInteractive", "-Command", script));
  }

  static List<String> command(String id, Path project, String model, String sandbox) {
    if (shellWrapper(id)) return powershellCommand(id);
    String override = System.getenv("IAMONITOR_" + id.toUpperCase(Locale.ROOT) + "_BIN");
    String executable = switch (id) {
      case "codex" -> "codex";
      case "claude" -> "claude";
      case "gemini" -> "gemini";
      case "cursor" -> "agent";
      case "copilot" -> "copilot";
      default -> throw new IllegalArgumentException("Ferramenta desconhecida: " + id);
    };
    List<String> result = new ArrayList<>();
    if (override != null && !override.isBlank()) {
      Path file = Path.of(override);
      if (!Files.isRegularFile(file)) throw new IllegalArgumentException("Executável não encontrado: " + override);
      if (override.matches("(?i).*\\.[cm]?js")) result.addAll(List.of("node", override));
      else if (override.matches("(?i).*\\.(cmd|bat)")) result.addAll(List.of("cmd.exe", "/c", override));
      else result.add(override);
    } else if ("codex".equals(id) && System.getProperty("os.name").toLowerCase().contains("win")
        && System.getenv("APPDATA") != null
        && Files.isRegularFile(Path.of(System.getenv("APPDATA"), "npm", "node_modules", "@openai", "codex", "bin", "codex.js"))) {
      result.addAll(List.of("node", Path.of(System.getenv("APPDATA"), "npm", "node_modules", "@openai", "codex", "bin", "codex.js").toString()));
    } else if (System.getProperty("os.name").toLowerCase().contains("win") && !"claude".equals(id)) {
      result.addAll(List.of("cmd.exe", "/c", executable));
    } else result.add(executable);
    switch (id) {
      case "codex" -> {
        result.addAll(List.of("exec", "--json", "--skip-git-repo-check", "-C", project.toString(), "-s", sandbox));
        if (!model.isBlank()) result.addAll(List.of("-m", model));
        result.add("-");
      }
      case "claude" -> {
        result.addAll(List.of("-p", "--output-format", "stream-json", "--verbose"));
        if (!model.isBlank()) result.addAll(List.of("--model", model));
        result.addAll(List.of("--permission-mode", "read-only".equals(sandbox) ? "plan" : "acceptEdits"));
      }
      case "gemini" -> {
        result.addAll(List.of("-p", "", "--output-format", "stream-json"));
        if (!model.isBlank()) result.addAll(List.of("--model", model));
        result.addAll(List.of("--approval-mode", "read-only".equals(sandbox) ? "plan" : "auto_edit"));
      }
      case "cursor" -> {
        result.addAll(List.of("-p", "--output-format", "stream-json"));
        if (!model.isBlank()) result.addAll(List.of("--model", model));
        if ("read-only".equals(sandbox)) result.addAll(List.of("--mode", "ask"));
      }
      case "copilot" -> {
        result.addAll(List.of("-p", "", "--output-format", "json", "--no-ask-user"));
        if (!model.isBlank()) result.addAll(List.of("--model", model));
        if ("read-only".equals(sandbox)) result.addAll(List.of("--allow-tool", "read", "--deny-tool", "write,shell"));
        else result.addAll(List.of("--allow-tool", "read,write,shell"));
      }
      default -> throw new IllegalArgumentException("Ferramenta desconhecida.");
    }
    return result;
  }

  static List<String> commandArguments(String id, Path project, String model, String sandbox) {
    List<String> command = command(id, project, model, sandbox);
    if (command.getFirst().equals("powershell.exe")) return List.of();
    int prefix = command.getFirst().equals("cmd.exe") ? 3 : command.getFirst().equals("node") ? 2 : 1;
    return command.subList(prefix, command.size());
  }

  static JsonNode normalize(String id, JsonNode raw) {
    if ("codex".equals(id)) return raw;
    ObjectNode event = raw.deepCopy();
    String type = raw.path("type").asText();
    event.put("provider_type", type);
    event.set("provider_event", raw);
    switch (id) {
      case "claude" -> {
        if ("result".equals(type)) {
          event.put("type", "turn.completed");
          JsonNode models = raw.path("modelUsage");
          long input = 0, cached = 0, output = 0;
          if (models.isObject() && !models.isEmpty()) {
            for (JsonNode model : models) {
              input += model.path("inputTokens").asLong() + model.path("cacheReadInputTokens").asLong()
                  + model.path("cacheCreationInputTokens").asLong();
              cached += model.path("cacheReadInputTokens").asLong();
              output += model.path("outputTokens").asLong();
            }
          } else {
            JsonNode usage = raw.path("usage");
            input = usage.path("input_tokens").asLong() + usage.path("cache_read_input_tokens").asLong()
                + usage.path("cache_creation_input_tokens").asLong();
            cached = usage.path("cache_read_input_tokens").asLong();
            output = usage.path("output_tokens").asLong();
          }
          if (models.isObject() && !models.isEmpty() || raw.path("usage").isObject()) {
            ObjectNode usage = event.putObject("usage");
            usage.put("input_tokens", input); usage.put("cached_input_tokens", cached);
            usage.put("output_tokens", output); usage.put("total_tokens", input + output);
          }
        } else if ("system".equals(type)) event.put("type", "thread.started");
        else event.put("type", "provider." + type);
      }
      case "gemini" -> {
        if ("result".equals(type)) {
          event.put("type", "turn.completed");
          JsonNode stats = raw.path("stats");
          if (stats.isObject() && stats.has("input_tokens") && stats.has("output_tokens")) {
            ObjectNode usage = event.putObject("usage");
            long input = stats.path("input_tokens").asLong(), output = stats.path("output_tokens").asLong();
            usage.put("input_tokens", input); usage.put("cached_input_tokens", stats.path("cached").asLong());
            usage.put("output_tokens", output); usage.put("total_tokens", stats.path("total_tokens").asLong(input + output));
          }
        } else if ("init".equals(type)) event.put("type", "thread.started");
        else event.put("type", "provider." + type);
      }
      default -> event.put("type", "provider." + type);
    }
    String detail = switch (id) {
      case "claude" -> claudeDetail(raw);
      case "gemini" -> raw.path("tool_name").asText("") + " "
          + raw.path("parameters").toString() + " " + raw.path("content").asText("");
      case "cursor" -> raw.has("tool_call") ? raw.path("tool_call").toString()
          : raw.path("message").toString();
      default -> raw.path("data").isMissingNode() ? raw.path("message").toString()
          : raw.path("data").toString();
    };
    event.put("summary", name(id) + " · " + type);
    event.put("detail", detail.length() > 4000 ? detail.substring(0, 4000) : detail);
    return event;
  }
  private static String claudeDetail(JsonNode raw) {
    StringBuilder detail = new StringBuilder();
    JsonNode content = raw.path("message").path("content");
    if (content.isArray()) for (JsonNode block : content) {
      if ("tool_use".equals(block.path("type").asText()))
        detail.append(block.path("name").asText()).append(' ').append(block.path("input")).append(' ');
      else if (block.path("text").isTextual()) detail.append(block.path("text").asText()).append(' ');
    }
    if (detail.isEmpty()) detail.append(raw.path("result").asText(raw.path("message").asText("")));
    return detail.toString().strip();
  }

}

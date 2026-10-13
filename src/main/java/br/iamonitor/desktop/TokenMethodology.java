package br.iamonitor.desktop;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingType;
import java.util.List;

final class TokenMethodology {
  record Explanation(String provider, String title, String how, String official,
      String limits, String source, String url) {}
  record Result(String provider, String name, int tokens, boolean encodingCount,
      String method) {}

  private static final Encoding O200K = Encodings.newDefaultEncodingRegistry()
      .getEncoding(EncodingType.O200K_BASE);

  private TokenMethodology() {}

  static Explanation explanation(String provider) {
    return switch (provider) {
      case "codex" -> new Explanation(provider, "OpenAI · Codex",
          "A OpenAI divide o texto em unidades por uma codificação de pares de bytes (BPE). "
              + "Palavras frequentes podem caber em um token; outras viram vários. "
              + "A codificação depende do modelo. Este simulador reproduz apenas a codificação o200k_base para texto puro.",
          "Para uma requisição completa, use a contagem de tokens de entrada da API Responses "
              + "com o modelo e o payload reais. Após a execução, consulte o uso reportado pela CLI.",
          "Mensagens, instruções, ferramentas, arquivos, imagens e turnos anteriores mudam o total. "
              + "O modelo selecionado no Codex pode usar outra codificação.",
          "OpenAI · Counting tokens",
          "https://developers.openai.com/api/docs/guides/token-counting");
      case "claude" -> new Explanation(provider, "Anthropic · Claude Code",
          "Claude usa um tokenizer associado ao modelo. A Anthropic recebe uma mensagem estruturada, "
              + "com instruções, ferramentas e outros blocos, e devolve a contagem de entrada pelo endpoint oficial. "
              + "Modelos diferentes podem tokenizar o mesmo texto de forma diferente.",
          "O endpoint POST /v1/messages/count_tokens aceita o modelo e o conteúdo da mensagem. "
              + "A resposta é uma estimativa oficial; o uso da mensagem executada é a referência final.",
          "O IAMonitor não possui o tokenizer de Claude nem envia seu texto à Anthropic. "
              + "A linha do simulador é apenas uma referência local o200k_base da OpenAI, não uma contagem Claude.",
          "Anthropic · Token counting",
          "https://platform.claude.com/docs/en/build-with-claude/token-counting");
      case "gemini" -> new Explanation(provider, "Google · Gemini CLI",
          "Gemini divide texto e outras modalidades em tokens segundo o modelo. "
              + "O Google usa cerca de quatro caracteres por token como regra didática para texto, "
              + "mas a proporção varia com idioma, código e símbolos.",
          "A API Gemini oferece countTokens para a entrada antes da chamada. "
              + "Após a execução, usageMetadata informa os tokens efetivamente processados.",
          "O simulador aplica somente a aproximação de quatro caracteres ao texto digitado. "
              + "Não executa countTokens e não inclui instruções da CLI, arquivos, imagens ou histórico.",
          "Google AI · Understand and count tokens",
          "https://ai.google.dev/gemini-api/docs/tokens");
      case "cursor" -> new Explanation(provider, "Cursor · Cursor Agent",
          "Cursor é a ferramenta que monta o pedido e seleciona ou recebe um modelo. "
              + "O texto é tokenizado pelo modelo subjacente, que pode vir de diferentes provedores. "
              + "Portanto não existe uma contagem universal de tokens do Cursor para qualquer texto.",
          "Escolha um modelo específico no Cursor e consulte o uso registrado pela ferramenta. "
              + "Para prever um pedido, use o contador oficial do provedor desse modelo, quando disponível.",
          "A linha do simulador é uma referência local o200k_base da OpenAI. "
              + "Ela não representa necessariamente o modelo usado pelo Cursor nem seu contexto adicional.",
          "Cursor · Models",
          "https://docs.cursor.com/models/");
      case "copilot" -> new Explanation(provider, "GitHub · Copilot CLI",
          "O Copilot CLI permite selecionar um modelo ou usar seleção automática. "
              + "A divisão em tokens segue o modelo efetivamente usado, não um tokenizer único do GitHub. "
              + "O Copilot também pode acrescentar instruções, histórico e resultados de ferramentas.",
          "Selecione o modelo com --model ou confira o modelo usado e os totais por modelo em /usage. "
              + "Para contar só um texto antes do envio, consulte o método do provedor do modelo escolhido.",
          "A linha do simulador é uma referência local o200k_base da OpenAI. "
              + "Ela não prevê tokens da sessão nem créditos do Copilot.",
          "GitHub Docs · Copilot CLI command reference",
          "https://docs.github.com/en/copilot/reference/copilot-cli-reference/cli-command-reference");
      default -> throw new IllegalArgumentException("Ferramenta desconhecida: " + provider);
    };
  }

  static List<Result> compare(String text) {
    if (text == null || text.isBlank())
      throw new IllegalArgumentException("Digite um texto para calcular.");
    int o200k = O200K.countTokens(text);
    int characters = text.codePointCount(0, text.length());
    int gemini = (characters + 3) / 4;
    return List.of(
        new Result("codex", Provider.name("codex"), o200k, true,
            "o200k_base · texto puro"),
        new Result("claude", Provider.name("claude"), o200k, false,
            "≈ referência o200k_base; não é Claude"),
        new Result("gemini", Provider.name("gemini"), gemini, false,
            "≈ caracteres ÷ 4; regra didática Google"),
        new Result("cursor", Provider.name("cursor"), o200k, false,
            "≈ referência o200k_base; modelo variável"),
        new Result("copilot", Provider.name("copilot"), o200k, false,
            "≈ referência o200k_base; modelo variável"));
  }
}

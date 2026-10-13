package br.iamonitor.desktop;

import java.text.NumberFormat;
import java.util.Locale;

final class TokenAccounting {
  record Assessment(String total, String status, String equation, String included, boolean finalNumber) {}
  record Parts(long inputWithoutCache, long cache, long output, long other) {}

  private TokenAccounting() {}

  static Parts parts(long input, long cache, long output, long reasoning, long total) {
    if (input < 0 || cache < 0 || cache > input || output < 0 || reasoning < 0
        || reasoning > output || total < 0 || input > Long.MAX_VALUE - output
        || total < input + output) return null;
    return new Parts(input - cache, cache, output, total - input - output);
  }

  static Assessment assess(Store.Run run, boolean reported) {
    return assess(run, reported, true);
  }

  static Assessment assess(Store.Run run, boolean reported, boolean auditable) {
    if (!reported) {
      String status = "running".equals(run.status())
          ? "AGUARDANDO · a CLI ainda não informou tokens"
          : "INDISPONÍVEL · a CLI não informou tokens";
      return new Assessment("—", status, "Não existe um total verificável para esta atividade.",
          "Nenhum valor é presumido como zero.", false);
    }

    if (!auditable)
      return new Assessment("—", "HISTÓRICO · sem eventos para conferir",
          "Total salvo anteriormente: " + num(run.totalTokens()) + " tokens.",
          "A contagem não pode ser confirmada pelos eventos da atividade.", false);
    long input = run.inputTokens(), output = run.outputTokens(), total = run.totalTokens();
    Parts breakdown = parts(input, run.cachedTokens(), output, run.reasoningTokens(), total);
    if (breakdown == null) {
      return new Assessment("—", "DIVERGÊNCIA · confira os eventos JSONL",
          "Total registrado: " + num(total) + "; entrada + saída: " + num(input) + " + "
              + num(output) + ".",
          "Os dados não fecham; não há número final confiável.", false);
    }

    String equation = num(breakdown.inputWithoutCache()) + " entrada sem cache + "
        + num(breakdown.cache()) + " cache + " + num(breakdown.output()) + " saída"
        + (breakdown.other() > 0 ? " + " + num(breakdown.other()) + " sem detalhamento" : "")
        + " = " + num(total) + " tokens";
    String included = "Entrada total reportada: " + num(input) + ", incluindo "
        + num(run.cachedTokens()) + " de cache."
        + (run.reasoningTokens() > 0 ? " " + num(run.reasoningTokens())
            + " de raciocínio já na saída." : "")
        + " A contagem não determina cobrança da assinatura.";
    if (run.missingUsage() > 0)
      return new Assessment("≥ " + num(total),
          "INCOMPLETO · " + run.missingUsage() + " resultado(s) sem contagem",
          equation, included + " O total real pode ser maior.", false);
    if ("running".equals(run.status()))
      return new Assessment(num(total), "PARCIAL · execução em andamento", equation,
          included + " O número ainda pode aumentar.", false);
    if (!"completed".equals(run.status()))
      return new Assessment(num(total), "PARCIAL · execução " + run.status(), equation,
          included + " A atividade não terminou normalmente.", false);
    return new Assessment(num(total), "FINAL · contagem da CLI nesta atividade", equation, included, true);
  }

  private static String num(long value) {
    return NumberFormat.getIntegerInstance(Locale.forLanguageTag("pt-BR")).format(value);
  }
}

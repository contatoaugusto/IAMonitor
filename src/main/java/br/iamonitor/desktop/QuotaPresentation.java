package br.iamonitor.desktop;

import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

final class QuotaPresentation {
  record View(String headline, String detail, String note) {}
  private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM HH:mm");

  private QuotaPresentation() {}

  static View describe(String provider, List<Store.QuotaSnapshot> snapshots) {
    if (!"codex".equals(provider))
      return new View("Cota indisponível para " + Provider.name(provider),
          "Os eventos desta CLI não informaram saldo da assinatura.",
          "Os tokens reportados acima medem a operação, não o débito do plano.");
    if (snapshots.isEmpty())
      return new View("Cota do Codex indisponível",
          "A conta ou a CLI não disponibilizou limites do ChatGPT para esta sessão.",
          "Os tokens reportados acima não indicam saldo da assinatura.");
    List<Store.QuotaSnapshot> latest = snapshots.stream()
        .filter(value -> "after".equals(value.phase()) || snapshots.stream().noneMatch(after ->
            "after".equals(after.phase()) && sameBucket(value, after)))
        .sorted(Comparator.comparing((Store.QuotaSnapshot value) -> !"codex".equals(value.limitId()))
            .thenComparing(Store.QuotaSnapshot::limitId).thenComparing(Store.QuotaSnapshot::window))
        .toList();
    StringBuilder detail = new StringBuilder();
    for (Store.QuotaSnapshot value : latest) {
      if (!detail.isEmpty()) detail.append('\n');
      if (!"codex".equals(value.limitId())) detail.append(value.limitId()).append(" · ");
      detail.append(windowLabel(value.windowMinutes())).append(": ")
          .append(percent(100 - value.usedPercent())).append(" restantes (")
          .append(percent(value.usedPercent())).append(" usados), renova ")
          .append(DATE.format(Instant.ofEpochSecond(value.resetsAt()).atZone(ZoneId.systemDefault())));
    }
    Store.QuotaSnapshot before = snapshots.stream().filter(value -> "before".equals(value.phase())
        && "codex".equals(value.limitId()) && "primary".equals(value.window())).findFirst().orElse(null);
    Store.QuotaSnapshot after = snapshots.stream().filter(value -> "after".equals(value.phase())
        && "codex".equals(value.limitId()) && "primary".equals(value.window())).findFirst().orElse(null);
    String headline = "Cota oficial do Codex";
    String note = "Tokens da operação não indicam saldo.";
    if (comparable(before, after)) {
      headline = "Cota oficial · variação: +" + number(after.usedPercent() - before.usedPercent()) + " p.p.";
      note = "Antes " + percent(before.usedPercent()) + "; depois " + percent(after.usedPercent())
          + ". Pode incluir outras atividades da conta.";
    } else if (after != null || before != null) {
      note = "Variação indisponível: janela ou leitura diferente.";
    }
    Store.QuotaSnapshot newest = latest.stream().max(Comparator.comparing(Store.QuotaSnapshot::capturedAt)).orElse(null);
    if (newest != null) note += " Leitura "
        + DATE.format(Instant.parse(newest.capturedAt()).atZone(ZoneId.systemDefault())) + ".";
    return new View(headline, detail.toString(), note);
  }

  static boolean comparable(Store.QuotaSnapshot before, Store.QuotaSnapshot after) {
    return before != null && after != null && sameBucket(before, after)
        && before.windowMinutes() == after.windowMinutes() && before.resetsAt() == after.resetsAt()
        && before.planType().equals(after.planType())
        && before.capturedAt().compareTo(after.capturedAt()) <= 0
        && after.usedPercent() >= before.usedPercent();
  }

  private static boolean sameBucket(Store.QuotaSnapshot first, Store.QuotaSnapshot second) {
    return first.limitId().equals(second.limitId()) && first.window().equals(second.window());
  }

  private static String percent(double value) {
    return number(value) + "%";
  }

  private static String number(double value) {
    NumberFormat format = NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-BR"));
    format.setMaximumFractionDigits(1);
    format.setMinimumFractionDigits(0);
    return format.format(value);
  }

  private static String windowLabel(int minutes) {
    if (minutes % 1440 == 0) return minutes / 1440 + " d";
    if (minutes % 60 == 0) return minutes / 60 + " h";
    return minutes + " min";
  }
}

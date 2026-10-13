package br.iamonitor.desktop;

import java.util.Set;

final class TokenSimulation {
  record Result(boolean available, long reported, long scenario, long input,
      long savings, long requested) {}

  private TokenSimulation() {}

  static Result calculate(Store.Detail detail, Set<String> excluded) {
    if (detail == null) return new Result(false, 0, 0, 0, 0, 0);
    Store.Run run = detail.run();
    boolean reported = run.totalTokens() > 0 || run.inputTokens() + run.outputTokens() > 0
        || detail.events().stream().anyMatch(event -> event.usage() != null);
    TokenAccounting.Assessment assessment =
        TokenAccounting.assess(run, reported, detail.usageAuditable());
    if ("—".equals(assessment.total())) return new Result(false, 0, 0, 0, 0, 0);
    long requested = 0;
    for (String key : Set.of("skill", "agent", "spec", "hook")) {
      if (!excluded.contains(key)) continue;
      EventLogic.Source source = detail.inputSources().get(key);
      if (source == null || source.estimatedTokens() == null) continue;
      long amount = Math.max(0, source.estimatedTokens());
      requested = amount > Long.MAX_VALUE - requested ? Long.MAX_VALUE : requested + amount;
    }
    long uncachedInput = Math.max(0, run.inputTokens() - run.cachedTokens());
    long savings = Math.min(requested, uncachedInput);
    return new Result(true, run.totalTokens(), run.totalTokens() - savings,
        run.inputTokens() - savings, savings, requested);
  }
}
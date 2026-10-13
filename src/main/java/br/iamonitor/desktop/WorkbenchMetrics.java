package br.iamonitor.desktop;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Read-only presentation data. Local artifact estimates never enter reported usage. */
final class WorkbenchMetrics {
  record Category(String name, int files, long estimatedTokens) {}
  record Snapshot(List<SessionFlow.Cycle> cycles, List<Category> categories,
                  int completed, int withoutUsage, int pending, boolean auditable) {}

  private WorkbenchMetrics() {}

  static Snapshot from(Store.Detail detail) {
    if (detail == null) return new Snapshot(List.of(), EventLogic.ARTIFACT_CATEGORIES.stream()
        .map(name -> new Category(name, 0, 0)).toList(), 0, 0, 0, false);
    SessionFlow.Snapshot flow = SessionFlow.from(detail.events(), detail.run().provider(),
        detail.usageAuditable());
    Map<String, long[]> groups = new LinkedHashMap<>();
    for (String category : EventLogic.ARTIFACT_CATEGORIES)
      groups.put(category, new long[2]);
    for (Store.Artifact artifact : detail.artifacts()) {
      String category = artifact.category() == null || artifact.category().isBlank()
          ? "Arquivo" : artifact.category();
      long[] count = groups.computeIfAbsent(category, ignored -> new long[2]);
      count[0]++;
      count[1] += Math.max(0, artifact.estimatedTokens());
    }
    List<Category> categories = new ArrayList<>();
    groups.forEach((name, values) -> categories.add(
        new Category(name, Math.toIntExact(values[0]), values[1])));
    return new Snapshot(List.copyOf(flow.cycles()), List.copyOf(categories),
        flow.completed(), flow.withoutUsage(), flow.pending(), detail.usageAuditable());
  }
}

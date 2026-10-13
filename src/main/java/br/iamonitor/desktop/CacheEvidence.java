package br.iamonitor.desktop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Only reported usage and observed event intervals; no inferred cache contents. */
final class CacheEvidence {
  record Field(String label, long tokens, String source) {}
  record Unit(int number, int startSequence, Store.Event result, List<Store.Event> interval,
              TokenAccounting.Parts parts, List<Field> providerFields) {
    EventLogic.Usage usage() { return result.usage(); }
  }
  record Snapshot(Store.Detail detail, List<Unit> units, long measuredCache,
                  boolean completeHistory) {}

  private static final ObjectMapper JSON = new ObjectMapper();

  private CacheEvidence() {}

  static Snapshot from(Store.Detail detail, Map<Integer, String> payloads, boolean completeHistory) {
    List<Unit> units = new ArrayList<>();
    List<Store.Event> interval = new ArrayList<>();
    long measuredCache = 0;
    for (Store.Event event : detail.events()) {
      if (!"turn.completed".equals(event.type())) {
        interval.add(event);
        continue;
      }
      EventLogic.Usage usage = event.usage();
      TokenAccounting.Parts parts = usage == null ? null : TokenAccounting.parts(
          usage.input(), usage.cached(), usage.output(), usage.reasoning(), usage.total());
      if (usage != null) measuredCache += usage.cached();
      int startSequence = interval.stream().filter(item -> "turn.started".equals(item.type()))
          .mapToInt(Store.Event::seq).findFirst().orElse(event.seq());
      units.add(new Unit(units.size() + 1, startSequence, event, List.copyOf(interval), parts,
          providerFields(detail.run().provider(), payloads.get(event.seq()))));
      interval.clear();
    }
    return new Snapshot(detail, List.copyOf(units), measuredCache, completeHistory);
  }

  static List<Field> providerFields(String provider, String payload) {
    if (payload == null || payload.isBlank()) return List.of();
    JsonNode event;
    try { event = JSON.readTree(payload); }
    catch (Exception error) { return List.of(); }
    JsonNode original = event.path("provider_event");
    List<Field> fields = new ArrayList<>();
    if ("claude".equals(provider)) {
      JsonNode models = original.path("modelUsage");
      if (models.isObject() && !models.isEmpty()) {
        models.fields().forEachRemaining(entry -> {
          add(fields, "Cache lido · " + entry.getKey(), entry.getValue().path("cacheReadInputTokens"),
              "modelUsage." + entry.getKey() + ".cacheReadInputTokens");
          add(fields, "Cache criado · " + entry.getKey(), entry.getValue().path("cacheCreationInputTokens"),
              "modelUsage." + entry.getKey() + ".cacheCreationInputTokens");
        });
      } else {
        add(fields, "Cache lido", original.path("usage").path("cache_read_input_tokens"),
            "usage.cache_read_input_tokens");
        add(fields, "Cache criado", original.path("usage").path("cache_creation_input_tokens"),
            "usage.cache_creation_input_tokens");
      }
    } else if ("gemini".equals(provider)) {
      add(fields, "Cache lido", original.path("stats").path("cached"), "stats.cached");
    } else {
      add(fields, "Cache lido", event.path("usage").path("cached_input_tokens"),
          "usage.cached_input_tokens");
      if (fields.isEmpty()) add(fields, "Cache lido", event.path("usage")
          .path("input_tokens_details").path("cached_tokens"),
          "usage.input_tokens_details.cached_tokens");
    }
    return List.copyOf(fields);
  }

  private static void add(List<Field> fields, String label, JsonNode value, String source) {
    if (value.isIntegralNumber() && value.asLong() >= 0)
      fields.add(new Field(label, value.asLong(), source));
  }
}

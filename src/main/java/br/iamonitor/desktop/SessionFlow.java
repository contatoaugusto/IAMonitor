package br.iamonitor.desktop;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class SessionFlow {
  record Cycle(int seq, String at, EventLogic.Usage usage, boolean completed, int messageEvents) {}
  record Snapshot(List<Cycle> cycles, int completed, int measured, int withoutUsage,
      int pending, int messageEvents, int unassignedMessages, String unit, boolean fullHistory) {}

  private SessionFlow() {}

  static Snapshot from(List<Store.Event> events, String provider, boolean fullHistory) {
    ArrayDeque<Store.Event> started = new ArrayDeque<>();
    List<Cycle> cycles = new ArrayList<>();
    int completed = 0, measured = 0, withoutUsage = 0, messages = 0, messagesInInterval = 0;
    for (Store.Event event : events) {
      if ("message".equals(event.kind())) {
        messages++;
        messagesInInterval++;
      } else if ("turn.started".equals(event.type())) {
        started.addLast(event);
      } else if ("turn.completed".equals(event.type())) {
        Store.Event beginning = started.pollFirst();
        completed++;
        if (event.usage() == null) withoutUsage++;
        else measured++;
        cycles.add(new Cycle(beginning == null ? event.seq() : beginning.seq(),
            event.at(), event.usage(), true, messagesInInterval));
        messagesInInterval = 0;
      }
    }
    int pending = started.size();
    for (Store.Event event : started) {
      cycles.add(new Cycle(event.seq(), event.at(), null, false, messagesInInterval));
      messagesInInterval = 0;
    }
    cycles.sort(Comparator.comparingInt(Cycle::seq));
    String unit = "codex".equals(provider) ? "turno" : "resultado de uso";
    return new Snapshot(List.copyOf(cycles), completed, measured, withoutUsage,
        pending, messages, messagesInInterval, unit, fullHistory);
  }
}

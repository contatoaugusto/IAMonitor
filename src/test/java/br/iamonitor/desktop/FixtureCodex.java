package br.iamonitor.desktop;

public final class FixtureCodex {
  public static void main(String[] args) throws Exception {
    String prompt = new String(System.in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
    System.out.println("{\"type\":\"thread.started\",\"thread_id\":\"java-fixture\"}");
    System.out.println("{\"type\":\"turn.started\"}");
    System.out.println("{\"type\":\"item.completed\",\"item\":{\"type\":\"command_execution\",\"command\":\"Get-Content README.md\"}}");
    if (prompt.contains("sleep")) Thread.sleep(5000);
    System.out.println("{\"type\":\"turn.completed\",\"usage\":{\"input_tokens\":120,\"cached_input_tokens\":20,\"output_tokens\":30,\"reasoning_tokens\":5,\"total_tokens\":150}}");
  }
}

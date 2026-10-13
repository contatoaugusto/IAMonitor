package br.iamonitor.desktop;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class FixtureQuota {
  public static void main(String[] args) throws Exception {
    BufferedReader input = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
    if (input.readLine() != null) {
      System.out.println("{\"id\":1,\"result\":{\"userAgent\":\"fixture\"}}");
      System.out.flush();
      input.readLine();
      String query = input.readLine();
      if (query == null) return;
      System.out.println("{\"id\":2,\"result\":{\"rateLimits\":{\"limitId\":\"codex\",\"primary\":{\"usedPercent\":25,\"windowDurationMins\":300,\"resetsAt\":1800000000}}}}");
      System.out.flush();
    }
  }
}

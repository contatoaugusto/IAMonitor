package br.iamonitor.desktop;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FlowExplainerTest {
  @Test void codexSeparatesLocalRemoteAndObservation() {
    String text = FlowExplainer.describe("codex");
    assertTrue(text.contains("AGENTS.md"));
    assertTrue(text.contains("inferência do modelo ocorre no serviço remoto"));
    assertTrue(text.contains("não significa que todo o repositório"));
    assertTrue(text.contains("não intercepta o corpo das requisições HTTPS"));
    assertTrue(text.contains("não são uma divisão por arquivo"));
  }

  @Test void otherProviderDoesNotInheritCodexInstructions() {
    String text = FlowExplainer.describe("claude");
    assertTrue(text.contains("Claude Code"));
    assertFalse(text.contains("AGENTS.md"));
  }
}

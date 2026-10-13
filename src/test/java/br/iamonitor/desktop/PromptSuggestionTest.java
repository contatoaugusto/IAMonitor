package br.iamonitor.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PromptSuggestionTest {
  @Test void leSomentePedidoDaPastaEscolhida(@TempDir Path root) throws Exception {
    Path project = Files.createDirectory(root.resolve("project"));
    assertNull(PromptSuggestion.read(project));
    Files.writeString(project.resolve("pedido.txt"), "  Crie uma página.  \n");
    assertEquals("Crie uma página.", PromptSuggestion.read(project));
  }

  @Test void rejeitaPedidoVazioOuGrande(@TempDir Path project) throws Exception {
    Path file = project.resolve("pedido.txt");
    Files.writeString(file, "  ");
    assertThrows(IOException.class, () -> PromptSuggestion.read(project));
    Files.writeString(file, "x".repeat((int) PromptSuggestion.MAX_BYTES + 1));
    assertThrows(IOException.class, () -> PromptSuggestion.read(project));
  }
}

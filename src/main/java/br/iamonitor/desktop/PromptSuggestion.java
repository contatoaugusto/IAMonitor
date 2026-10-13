package br.iamonitor.desktop;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

final class PromptSuggestion {
  static final long MAX_BYTES = 64 * 1024;

  private PromptSuggestion() {}

  static String read(Path project) throws IOException {
    Path file = project.resolve("pedido.txt");
    if (!Files.isRegularFile(file)) return null;
    if (Files.size(file) > MAX_BYTES) throw new IOException("pedido.txt excede 64 KB");
    String prompt = Files.readString(file, StandardCharsets.UTF_8).strip();
    if (prompt.isEmpty()) throw new IOException("pedido.txt está vazio");
    return prompt;
  }
}

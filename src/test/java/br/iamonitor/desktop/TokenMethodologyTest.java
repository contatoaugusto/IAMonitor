package br.iamonitor.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

class TokenMethodologyTest {
  @Test void comparaCincoFerramentasSemConfundirProxyComContagemOficial() {
    var results = TokenMethodology.compare("Olá, mundo! 👋");
    assertEquals(Provider.IDS, results.stream().map(TokenMethodology.Result::provider).toList());
    assertTrue(results.getFirst().encodingCount());
    assertEquals(new EventLogic().tokens("Olá, mundo! 👋"), results.getFirst().tokens());
    assertEquals(results.getFirst().tokens(), results.get(1).tokens());
    assertEquals(results.getFirst().tokens(), results.get(3).tokens());
    assertEquals(results.getFirst().tokens(), results.get(4).tokens());
    assertFalse(results.get(1).encodingCount());
    assertEquals(4, results.get(2).tokens()); // 13 caracteres Unicode, arredondados por 4.
    assertTrue(results.get(2).method().contains("≈"));
  }

  @Test void exigeTextoEIdentificaMetodoOficialDeCadaFerramenta() {
    assertThrows(IllegalArgumentException.class, () -> TokenMethodology.compare("  \n"));
    for (String provider : Provider.IDS) {
      var explanation = TokenMethodology.explanation(provider);
      assertEquals(provider, explanation.provider());
      assertTrue(explanation.how().length() > 100);
      assertTrue(explanation.official().length() > 80);
      assertTrue(explanation.url().startsWith("https://"));
    }
    assertTrue(TokenMethodology.explanation("cursor").how().contains("modelo subjacente"));
    assertTrue(TokenMethodology.explanation("copilot").how().contains("modelo"));
  }

  @Test void abaCalculaLocalmenteAposClique() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      var panel = TokenMethodologyPanel.create("gemini");
      var combos = children(panel, JComboBox.class);
      assertEquals(1, combos.size());
      assertTrue(combos.getFirst().getSelectedItem().toString().contains("Gemini"));
      combos.getFirst().setSelectedIndex(1);
      assertTrue(children(panel, JTextArea.class).stream()
          .anyMatch(area -> area.getText().contains("POST /v1/messages/count_tokens")));
      var areas = children(panel, JTextArea.class);
      var input = areas.stream().filter(area -> area.isEditable()).findFirst().orElseThrow();
      input.setText("Olá");
      var calculate = children(panel, JButton.class).stream()
          .filter(button -> button.getText().equals("Calcular tokens")).findFirst().orElseThrow();
      calculate.doClick();
      assertTrue(children(panel, JLabel.class).stream()
          .anyMatch(label -> label.getText().contains("o200k_base")));
      assertTrue(children(panel, JLabel.class).stream()
          .anyMatch(label -> label.getText().contains("GitHub Copilot CLI")));
    });
  }

  private static <T extends Component> List<T> children(Component root, Class<T> type) {
    List<T> found = new ArrayList<>();
    if (type.isInstance(root)) found.add(type.cast(root));
    if (root instanceof Container container)
      for (Component child : container.getComponents()) found.addAll(children(child, type));
    return found;
  }
}

package br.iamonitor.desktop;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class TokenAccountingTest {
  private static Store.Run run(String status, long input, long cache, long output,
      long reasoning, long total, long missing) {
    return new Store.Run("id", "C:/project", "pedido", null, "read-only", "codex", status,
        "2026-10-09T00:00:00Z", null, null, null, input, cache, output, reasoning,
        total, missing, 0, 0);
  }

  @Test void destacaTotalFinalSemSomarCacheERaciocinio() {
    var count = TokenAccounting.assess(run("completed", 1200, 300, 450, 150, 1650, 0), true);
    assertEquals("1.650", count.total());
    assertTrue(count.finalNumber());
    assertEquals("900 entrada sem cache + 300 cache + 450 saída = 1.650 tokens", count.equation());
    assertTrue(count.included().contains("Entrada total reportada: 1.200, incluindo 300 de cache"));
    assertTrue(count.included().contains("150 de raciocínio já na saída"));
    assertTrue(count.included().contains("não determina cobrança da assinatura"));
  }

  @Test void decompoeExemploComCacheAltoSemDuplicarEntrada() {
    var count = TokenAccounting.assess(run("completed", 4709555, 4635904, 15200,
        0, 4724755, 0), true);
    assertEquals("73.651 entrada sem cache + 4.635.904 cache + 15.200 saída = 4.724.755 tokens",
        count.equation());
    assertEquals(new TokenAccounting.Parts(73651, 4635904, 15200, 0),
        TokenAccounting.parts(4709555, 4635904, 15200, 0, 4724755));
    assertNull(TokenAccounting.parts(100, 101, 20, 0, 120));
    assertNull(TokenAccounting.parts(100, 20, 20, 21, 120));
  }

  @Test void preservaTotalDaCliComParcelaSemDetalhamento() {
    var count = TokenAccounting.assess(run("completed", 100, 30, 20, 0, 124, 0), true);
    assertEquals("124", count.total());
    assertTrue(count.equation().contains("4 sem detalhamento"));
    assertTrue(count.finalNumber());
  }

  @Test void distingueParcialIncompletoIndisponivelEDivergente() {
    var partial = TokenAccounting.assess(run("running", 120, 20, 30, 0, 150, 0), true);
    assertEquals("150", partial.total());
    assertTrue(partial.status().contains("PARCIAL"));
    assertFalse(partial.finalNumber());

    var incomplete = TokenAccounting.assess(run("completed", 120, 20, 30, 0, 150, 1), true);
    assertEquals("≥ 150", incomplete.total());
    assertTrue(incomplete.status().contains("INCOMPLETO"));

    var unavailable = TokenAccounting.assess(run("completed", 0, 0, 0, 0, 0, 0), false);
    assertEquals("—", unavailable.total());
    assertTrue(unavailable.status().contains("INDISPONÍVEL"));

    var inconsistent = TokenAccounting.assess(run("completed", 120, 20, 30, 0, 140, 0), true);
    assertEquals("—", inconsistent.total());
    assertTrue(inconsistent.status().contains("DIVERGÊNCIA"));
  }
}

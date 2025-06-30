package org.ek9.lang;


import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LevenshteinTest {
  private final Levenshtein underTest = new Levenshtein();

  @Test
  void testBasic() {
    int cost = underTest.costOfMatch("The Same", "The Same");
    assertEquals(0, cost);

    cost = underTest.costOfMatch("The Same", "The Sam.");
    assertEquals(2, cost);

    cost = underTest.costOfMatch("The Same", "The same");
    assertEquals(1, cost);

    cost = underTest.costOfMatch("The Same", "Not the same");
    assertEquals(6, cost);

    cost = underTest.costOfMatch("Circle", "Cicles");
    assertEquals(4, cost);

    cost = underTest.costOfMatch("Circle", "circle");
    assertEquals(1, cost);

  }
}

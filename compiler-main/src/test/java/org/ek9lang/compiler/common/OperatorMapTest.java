package org.ek9lang.compiler.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.core.CompilerException;
import org.junit.jupiter.api.Test;

class OperatorMapTest {

  private final OperatorMap underTest = new OperatorMap();

  @Test
  void testForwardMapping() {
    for (var forwardKey : underTest.getForwardKeys()) {
      assertTrue(underTest.checkForward(forwardKey));
      final var mappedToValue = underTest.getForward(forwardKey);
      final var checkKey = underTest.getBackward(mappedToValue);
      assertEquals(forwardKey, checkKey);
    }
  }

  @Test
  void testBackwardMapping() {
    for (var backwardKey : underTest.getBackwardKeys()) {
      assertTrue(underTest.checkBackward(backwardKey));
      final var mappedToValue = underTest.getBackward(backwardKey);
      final var checkKey = underTest.getForward(mappedToValue);
      assertEquals(backwardKey, checkKey);
    }
  }

  @Test
  void testExpectSingleParameter() {
    assertTrue(underTest.expectsParameter("<"));
    assertFalse(underTest.expectsParameter("~"));
  }

  @Test
  void testExpectNoParameter() {
    assertFalse(underTest.expectsZeroParameters("<"));
    assertTrue(underTest.expectsZeroParameters("~"));
  }

  @Test
  void testInvalidMapping() {
    assertThrows(CompilerException.class, () -> underTest.getForward("NO-SUCH"));
    assertThrows(CompilerException.class, () -> underTest.getBackward("NO-SUCH"));
  }
}

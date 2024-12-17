package org.ek9lang.compiler.phase5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ComplexityCounterTest {

  @Test
  void testEmpty() {
    final var underTest = new ComplexityCounter();
    assertTrue(underTest.isEmpty());
  }

  @Test
  void testExceptionOnEmpty() {
    final var underTest = new ComplexityCounter();

    assertThrows(IllegalArgumentException.class, underTest::pop);
    assertThrows(IllegalArgumentException.class, underTest::incrementComplexity);
    assertThrows(IllegalArgumentException.class, () -> underTest.incrementComplexity(2));
    assertThrows(IllegalArgumentException.class, underTest::peek);
  }

  @Test
  void testPushOperation() {
    final var underTest = new ComplexityCounter();

    final var value1 = underTest.push();
    assertEquals(0, value1.intValue());

    final var value2 = underTest.push();
    assertEquals(0, value2.intValue());

    underTest.incrementComplexity();
    //Check original has not been modified and only the second on the stack has
    assertEquals(0, value1.intValue());
    assertEquals(1, value2.intValue());

    underTest.incrementComplexity(3);
    //Check original has not been modified and only the second on the stack has added all the increments
    assertEquals(0, value1.intValue());
    assertEquals(4, value2.intValue());

  }

  @Test
  void testPushAndPopOperations() {
    final var underTest = new ComplexityCounter();

    final var value1A = underTest.push();
    assertEquals(0, value1A.intValue());
    underTest.incrementComplexity();
    assertEquals(1, value1A.intValue());

    final var value2A = underTest.push();
    assertEquals(0, value2A.intValue());
    underTest.incrementComplexity(3);

    assertEquals(1, value1A.intValue());
    assertEquals(3, value2A.intValue());

    final var value2B = underTest.pop();
    assertEquals(value2A, value2B);

    final var value1B = underTest.pop();
    assertEquals(value1A, value1B);


  }
}

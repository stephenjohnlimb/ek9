package org.company.dept;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * The static 'of' and 'from' methods are what the EK9 will call when
 * generating asm code from the EK9 code.
 * Also, these are the methods that an APi developer will be able to use when
 * writing Java classes that can be exposed via EK9.
 */
class StringExampleTest {

  @Test
  void testCreation() {
    final var example1 = StringExample.of("SomeValue");
    assertNotNull(example1);
    final var example1JavaString = StringExample.from(example1);
    assertEquals("SomeValue", example1JavaString);

    final var asUpperCase = example1.upperCase();
    assertNotNull(asUpperCase);

  }

  @Test
  void testMethodCall() {
    final var example1 = StringExample.of("SomeValue");

    final var asUpperCase = example1.upperCase();
    assertEquals("SOMEVALUE", StringExample.from(asUpperCase));

  }
}

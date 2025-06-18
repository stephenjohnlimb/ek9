package org.company.dept;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.ek9tooling.introspection.Ek9ExternExtractor;
import org.junit.jupiter.api.Test;


class AnnotationTest {
  final Ek9ExternExtractor underTest = new Ek9ExternExtractor();

  @Test
  void testValidEk9Annotations() {
    final var packageName = "org.company.dept";

    final var possibleEk9Interface = underTest.apply(packageName);
    assertNotNull(possibleEk9Interface.ek9Interface());
    assertNull(possibleEk9Interface.errorMessage());

    System.out.println(possibleEk9Interface.ek9Interface());

  }

  @Test
  void testInvalidEk9Annotations() {
    final var packageName = "org.company.nosuchdept";

    final var possibleEk9Interface = underTest.apply(packageName);
    assertNull(possibleEk9Interface.ek9Interface());
    assertNotNull(possibleEk9Interface.errorMessage());

  }
}

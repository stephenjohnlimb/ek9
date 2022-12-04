package org.ek9lang.core.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class HolderTest {

  @Test
  void testDefaultEmptyHolder() {
    var underTest = new Holder<Integer>();
    assertTrue(underTest.isEmpty());
  }

  @Test
  void testWithValueHolder() {
    var underTest = new Holder<Integer>(5);
    assertValueHeld(5, underTest);
  }

  @Test
  void testMutationToEmptyValueHolder() {
    var underTest = new Holder<Integer>(5);
    assertValueHeld(5, underTest);
    underTest.accept(Optional.empty());
    assertTrue(underTest.get().isEmpty());
  }

  @Test
  void testMutationToNewValueValueHolder() {
    var underTest = new Holder<Integer>(5);
    assertValueHeld(5, underTest);
    underTest.accept(Optional.of(89));
    assertValueHeld(89, underTest);
  }

  private void assertValueHeld(int expected, Supplier<Optional<Integer>> possibleValueSupplier) {
    var possibleValue = possibleValueSupplier.get();
    assertTrue(possibleValue.isPresent());
    assertEquals(expected, possibleValue.get());
  }
}

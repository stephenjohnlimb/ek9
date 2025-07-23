package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

@SuppressWarnings({"checkstyle:MethodName", "checkstyle:AbbreviationAsWordInName", "checkstyle:TypeName"})
class _Iterator_7E0CA90C1F947ECE11C43ED0BB21B854FFD82455CECBC0C3EA4CC4A6EA343408Test extends Common {

  // Type alias for cleaner code

  // Factory methods for cleaner object creation
  private static _Iterator_7E0CA90C1F947ECE11C43ED0BB21B854FFD82455CECBC0C3EA4CC4A6EA343408 iterCharacter() {
    return new _Iterator_7E0CA90C1F947ECE11C43ED0BB21B854FFD82455CECBC0C3EA4CC4A6EA343408();
  }
  
  private static _Iterator_7E0CA90C1F947ECE11C43ED0BB21B854FFD82455CECBC0C3EA4CC4A6EA343408 iterCharacter(Character value) {
    return new _Iterator_7E0CA90C1F947ECE11C43ED0BB21B854FFD82455CECBC0C3EA4CC4A6EA343408(value);
  }
  
  private static _Iterator_7E0CA90C1F947ECE11C43ED0BB21B854FFD82455CECBC0C3EA4CC4A6EA343408 iterCharacterFromBase(Iterator base) {
    return _Iterator_7E0CA90C1F947ECE11C43ED0BB21B854FFD82455CECBC0C3EA4CC4A6EA343408._of(base);
  }
  
  // Helper methods for set/unset assertions
  private void assertIteratorSet(_Iterator_7E0CA90C1F947ECE11C43ED0BB21B854FFD82455CECBC0C3EA4CC4A6EA343408 iterator) {
    assertSet.accept(iterator);
    assertTrue.accept(iterator.hasNext());
  }
  
  private void assertIteratorUnset(_Iterator_7E0CA90C1F947ECE11C43ED0BB21B854FFD82455CECBC0C3EA4CC4A6EA343408 iterator) {
    assertUnset.accept(iterator);
    assertFalse.accept(iterator.hasNext());
  }

  @Test
  void testConstructionAndBasicOperations() {
    // Test default constructor
    final var defaultIterator = iterCharacter();
    assertIteratorUnset(defaultIterator);

    // Test constructor with single element
    final var char1 = Character._of('A');
    final var singleElementIterator = iterCharacter(char1);
    assertIteratorSet(singleElementIterator);
    assertEquals(char1, singleElementIterator.next());
    assertIteratorUnset(singleElementIterator);
  }

  @Test
  void testFactoryMethodsAndDelegation() {
    // Test _of() factory method - empty iterator
    final var unset1 = _Iterator_7E0CA90C1F947ECE11C43ED0BB21B854FFD82455CECBC0C3EA4CC4A6EA343408._of();
    assertNotNull(unset1);
    assertIteratorUnset(unset1);

    // Test _of(null) factory method
    final var unset2 = iterCharacterFromBase(null);
    assertNotNull(unset2);
    assertIteratorUnset(unset2);

    // Test _of(Iterator) factory method - from base Iterator
    final var testChar = Character._of("S");
    final var set1 = iterCharacterFromBase(Iterator._of(testChar));
    assertNotNull(set1);
    assertIteratorSet(set1);
    assertEquals(Character._of('S'), set1.next());
    assertIteratorUnset(set1);
  }

  @Test
  void testOperatorsAndTypeSafety() {
    // Test equality and operators
    final var char1 = Character._of('A');
    final var iter1 = iterCharacter(char1);
    final var iter2 = iterCharacter(char1);
    assertFalse.accept(iter1._eq(iter2));
    assertNotEquals(iter1, iter2);
    assertNotEquals(iter1.hashCode(), iter2.hashCode());
  }
}

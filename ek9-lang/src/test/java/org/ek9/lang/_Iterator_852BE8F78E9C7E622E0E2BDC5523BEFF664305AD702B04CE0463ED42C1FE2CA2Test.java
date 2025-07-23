package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

@SuppressWarnings({"checkstyle:MethodName", "checkstyle:AbbreviationAsWordInName", "checkstyle:TypeName"})
class _Iterator_852BE8F78E9C7E622E0E2BDC5523BEFF664305AD702B04CE0463ED42C1FE2CA2Test extends Common {

  // Type alias for cleaner code

  // Factory methods for cleaner object creation
  private static _Iterator_852BE8F78E9C7E622E0E2BDC5523BEFF664305AD702B04CE0463ED42C1FE2CA2 iterString() {
    return new _Iterator_852BE8F78E9C7E622E0E2BDC5523BEFF664305AD702B04CE0463ED42C1FE2CA2();
  }
  
  private static _Iterator_852BE8F78E9C7E622E0E2BDC5523BEFF664305AD702B04CE0463ED42C1FE2CA2 iterString(String value) {
    return new _Iterator_852BE8F78E9C7E622E0E2BDC5523BEFF664305AD702B04CE0463ED42C1FE2CA2(value);
  }
  
  private static _Iterator_852BE8F78E9C7E622E0E2BDC5523BEFF664305AD702B04CE0463ED42C1FE2CA2 iterStringFromBase(Iterator base) {
    return _Iterator_852BE8F78E9C7E622E0E2BDC5523BEFF664305AD702B04CE0463ED42C1FE2CA2._of(base);
  }
  
  // Helper methods for set/unset assertions
  private void assertIteratorSet(_Iterator_852BE8F78E9C7E622E0E2BDC5523BEFF664305AD702B04CE0463ED42C1FE2CA2 iterator) {
    assertSet.accept(iterator);
    assertTrue.accept(iterator.hasNext());
  }
  
  private void assertIteratorUnset(_Iterator_852BE8F78E9C7E622E0E2BDC5523BEFF664305AD702B04CE0463ED42C1FE2CA2 iterator) {
    assertUnset.accept(iterator);
    assertFalse.accept(iterator.hasNext());
  }

  @Test
  void testConstructionAndBasicOperations() {
    // Test default constructor
    final var defaultIterator = iterString();
    assertIteratorUnset(defaultIterator);

    // Test constructor with single element
    final var string1 = String._of("Hello");
    final var singleElementIterator = iterString(string1);
    assertIteratorSet(singleElementIterator);
    assertEquals(string1, singleElementIterator.next());
    assertIteratorUnset(singleElementIterator);
  }

  @Test
  void testFactoryMethodsAndDelegation() {
    // Test _of() factory method - empty iterator
    final var unset1 = _Iterator_852BE8F78E9C7E622E0E2BDC5523BEFF664305AD702B04CE0463ED42C1FE2CA2._of();
    assertNotNull(unset1);
    assertIteratorUnset(unset1);

    // Test _of(null) factory method
    final var unset2 = iterStringFromBase(null);
    assertNotNull(unset2);
    assertIteratorUnset(unset2);

    // Test _of(Iterator) factory method - from base Iterator
    final var testString = String._of("Test");
    final var set1 = iterStringFromBase(Iterator._of(testString));
    assertNotNull(set1);
    assertIteratorSet(set1);
    assertEquals(testString, set1.next());
    assertIteratorUnset(set1);
  }

  @Test
  void testOperatorsAndEquality() {
    final var string1 = String._of("Test");
    final var iter1 = iterString(string1);
    final var iter2 = iterString(string1);
    final var emptyIter = iterString();

    // Iterator instances are not equal even with same content
    assertFalse.accept(iter1._eq(iter2));
    assertNotEquals(iter1, iter2);
    assertNotEquals(iter1.hashCode(), iter2.hashCode());

    // Test _hashcode operator
    assertSet.accept(iter1._hashcode());
    assertSet.accept(emptyIter._hashcode());
  }

}
package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

@SuppressWarnings({"checkstyle:MethodName", "checkstyle:AbbreviationAsWordInName", "checkstyle:TypeName"})
class _Iterator_6E53B4C9D56633C9606BBF50DB03B8B3D0E6FDD8FC70D7C04705495D86D9FD65Test extends Common {

  // Type alias for cleaner code

  // Factory methods for cleaner object creation
  private static _Iterator_6E53B4C9D56633C9606BBF50DB03B8B3D0E6FDD8FC70D7C04705495D86D9FD65 iterBoolean() {
    return new _Iterator_6E53B4C9D56633C9606BBF50DB03B8B3D0E6FDD8FC70D7C04705495D86D9FD65();
  }
  
  private static _Iterator_6E53B4C9D56633C9606BBF50DB03B8B3D0E6FDD8FC70D7C04705495D86D9FD65 iterBoolean(Boolean value) {
    return new _Iterator_6E53B4C9D56633C9606BBF50DB03B8B3D0E6FDD8FC70D7C04705495D86D9FD65(value);
  }
  
  private static _Iterator_6E53B4C9D56633C9606BBF50DB03B8B3D0E6FDD8FC70D7C04705495D86D9FD65 iterBooleanFromBase(Iterator base) {
    return _Iterator_6E53B4C9D56633C9606BBF50DB03B8B3D0E6FDD8FC70D7C04705495D86D9FD65._of(base);
  }
  
  // Helper methods for set/unset assertions
  private void assertIteratorSet(_Iterator_6E53B4C9D56633C9606BBF50DB03B8B3D0E6FDD8FC70D7C04705495D86D9FD65 iterator) {
    assertSet.accept(iterator);
    assertTrue.accept(iterator.hasNext());
  }
  
  private void assertIteratorUnset(_Iterator_6E53B4C9D56633C9606BBF50DB03B8B3D0E6FDD8FC70D7C04705495D86D9FD65 iterator) {
    assertUnset.accept(iterator);
    assertFalse.accept(iterator.hasNext());
  }

  @Test
  void testConstructionAndBasicOperations() {
    // Test default constructor
    final var defaultIterator = iterBoolean();
    assertIteratorUnset(defaultIterator);

    // Test constructor with single element
    final var bool1 = Boolean._of(true);
    final var singleElementIterator = iterBoolean(bool1);
    assertIteratorSet(singleElementIterator);
    assertEquals(bool1, singleElementIterator.next());
    assertIteratorUnset(singleElementIterator);
  }

  @Test
  void testFactoryMethodsAndDelegation() {
    // Test _of() factory method - empty iterator
    final var unset1 = _Iterator_6E53B4C9D56633C9606BBF50DB03B8B3D0E6FDD8FC70D7C04705495D86D9FD65._of();
    assertNotNull(unset1);
    assertIteratorUnset(unset1);

    // Test _of(null) factory method
    final var unset2 = iterBooleanFromBase(null);
    assertNotNull(unset2);
    assertIteratorUnset(unset2);

    // Test _of(Iterator) factory method - from base Iterator
    final var testBool = Boolean._of(false);
    final var set1 = iterBooleanFromBase(Iterator._of(testBool));
    assertNotNull(set1);
    assertIteratorSet(set1);
    assertEquals(testBool, set1.next());
    assertIteratorUnset(set1);
  }

  @Test
  void testOperatorsAndTypeSafety() {
    // Test equality and operators
    final var bool1 = Boolean._of(true);
    final var iter1 = iterBoolean(bool1);
    final var iter2 = iterBoolean(bool1);
    assertFalse.accept(iter1._eq(iter2));
    assertNotEquals(iter1, iter2);
    assertNotEquals(iter1.hashCode(), iter2.hashCode());
    
    // Test iteration over multiple Boolean values
    final var booleanList = new java.util.ArrayList<Any>();
    booleanList.add(Boolean._of(true));
    booleanList.add(Boolean._of(false));
    booleanList.add(Boolean._of(true));
    
    final var multiElementIterator = iterBooleanFromBase(Iterator._of(booleanList));
    assertIteratorSet(multiElementIterator);
    assertEquals(Boolean._of(true), multiElementIterator.next());
    assertEquals(Boolean._of(false), multiElementIterator.next());
    assertEquals(Boolean._of(true), multiElementIterator.next());
    assertIteratorUnset(multiElementIterator);
  }
}
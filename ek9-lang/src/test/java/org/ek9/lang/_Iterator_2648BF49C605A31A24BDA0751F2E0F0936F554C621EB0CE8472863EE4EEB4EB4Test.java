package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Test Iterator of Integer parameterized type.
 * Tests delegation pattern and Integer-specific type safety.
 */
@SuppressWarnings({"checkstyle:MethodName", "checkstyle:AbbreviationAsWordInName", "checkstyle:TypeName"})
class _Iterator_2648BF49C605A31A24BDA0751F2E0F0936F554C621EB0CE8472863EE4EEB4EB4Test extends Common {

  // Type alias for cleaner code

  // Factory methods for cleaner object creation
  private static _Iterator_2648BF49C605A31A24BDA0751F2E0F0936F554C621EB0CE8472863EE4EEB4EB4 iterInteger() {
    return new _Iterator_2648BF49C605A31A24BDA0751F2E0F0936F554C621EB0CE8472863EE4EEB4EB4();
  }
  
  private static _Iterator_2648BF49C605A31A24BDA0751F2E0F0936F554C621EB0CE8472863EE4EEB4EB4 iterInteger(Integer value) {
    return new _Iterator_2648BF49C605A31A24BDA0751F2E0F0936F554C621EB0CE8472863EE4EEB4EB4(value);
  }
  
  private static _Iterator_2648BF49C605A31A24BDA0751F2E0F0936F554C621EB0CE8472863EE4EEB4EB4 iterIntegerFromBase(Iterator base) {
    return _Iterator_2648BF49C605A31A24BDA0751F2E0F0936F554C621EB0CE8472863EE4EEB4EB4._of(base);
  }
  
  // Helper methods for set/unset assertions
  private void assertIteratorSet(_Iterator_2648BF49C605A31A24BDA0751F2E0F0936F554C621EB0CE8472863EE4EEB4EB4 iterator) {
    assertSet.accept(iterator);
    assertTrue.accept(iterator.hasNext());
  }
  
  private void assertIteratorUnset(_Iterator_2648BF49C605A31A24BDA0751F2E0F0936F554C621EB0CE8472863EE4EEB4EB4 iterator) {
    assertUnset.accept(iterator);
    assertFalse.accept(iterator.hasNext());
  }

  @Test
  void testConstructionAndBasicOperations() {
    // Test default constructor
    final var defaultIterator = iterInteger();
    assertNotNull(defaultIterator);
    assertIteratorUnset(defaultIterator);

    // Test value constructor
    final var valueIterator = iterInteger(Integer._of(42));
    assertNotNull(valueIterator);
    assertIteratorSet(valueIterator);
  }

  @Test
  void testFactoryMethodsAndDelegation() {
    // Test _of() factory method - empty iterator
    final var emptyIterator = _Iterator_2648BF49C605A31A24BDA0751F2E0F0936F554C621EB0CE8472863EE4EEB4EB4._of();
    assertNotNull(emptyIterator);
    assertIteratorUnset(emptyIterator);

    // Test _of(null) factory method
    final var fromNull = iterIntegerFromBase(null);
    assertNotNull(fromNull);
    assertIteratorUnset(fromNull);

    // Test _of(Iterator) factory method - from base Iterator
    final var baseIterator = Iterator._of(Integer._of(100));
    final var fromBase = iterIntegerFromBase(baseIterator);
    assertNotNull(fromBase);
    assertIteratorSet(fromBase);
  }

  @Test
  void testOperatorsAndTypeSafety() {
    // Test operators with different states
    final var iterator1 = iterInteger();
    final var iterator2 = iterInteger();
    final var iteratorWithValue = iterInteger(Integer._of(42));

    // Test equality functionality
    assertNotNull(iterator1._eq(iterator2));
    assertNotNull(iterator1._eq(iterator1));

    // Test _isSet operator (should return hasNext result)
    assertEquals(iteratorWithValue.hasNext(), iteratorWithValue._isSet());

    // Test _hashcode operator
    assertNotNull(iteratorWithValue._hashcode());
    
    // Test Integer type safety
    final var testValue = Integer._of(123);
    final var typeIterator = iterInteger(testValue);
    assertTrue.accept(typeIterator.hasNext());
    final var result = typeIterator.next();
    assertEquals(testValue, result);
    assertEquals(Integer.class, result.getClass());
    assertIteratorUnset(typeIterator);
  }

}
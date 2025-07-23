package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Test Iterator of DictEntry of (String, Integer) parameterized type.
 * Tests nested parameterization pattern and type safety for key-value pair iteration.
 */
@SuppressWarnings({"checkstyle:MethodName", "checkstyle:AbbreviationAsWordInName", "checkstyle:TypeName"})
class _Iterator_48D70134B2562F23E0D9F143A4A9BF02060D37448527B5CDB59DBC1C05F5D826Test extends Common {

  // Type aliases for cleaner code

  private static final Class<_DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E> DICT_ENTRY_SI = 
      _DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E.class;

  // Test data
  private final String testKey1 = String._of("key1");
  private final Integer testValue1 = Integer._of(10);
  
  // Factory methods for cleaner object creation
  private static _Iterator_48D70134B2562F23E0D9F143A4A9BF02060D37448527B5CDB59DBC1C05F5D826 iterDictEntryStrInt() {
    return new _Iterator_48D70134B2562F23E0D9F143A4A9BF02060D37448527B5CDB59DBC1C05F5D826();
  }
  
  private static _Iterator_48D70134B2562F23E0D9F143A4A9BF02060D37448527B5CDB59DBC1C05F5D826 iterDictEntryStrInt(
      _DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E entry) {
    return new _Iterator_48D70134B2562F23E0D9F143A4A9BF02060D37448527B5CDB59DBC1C05F5D826(entry);
  }
  
  private static _Iterator_48D70134B2562F23E0D9F143A4A9BF02060D37448527B5CDB59DBC1C05F5D826 iterDictEntryStrIntFromBase(Iterator base) {
    return _Iterator_48D70134B2562F23E0D9F143A4A9BF02060D37448527B5CDB59DBC1C05F5D826._of(base);
  }
  
  private _DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E testEntry() {
    return _DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E._of(testKey1, testValue1);
  }
  
  // Helper methods for set/unset assertions
  private void assertIteratorSet(_Iterator_48D70134B2562F23E0D9F143A4A9BF02060D37448527B5CDB59DBC1C05F5D826 iterator) {
    assertSet.accept(iterator);
    assertTrue.accept(iterator.hasNext());
  }
  
  private void assertIteratorUnset(_Iterator_48D70134B2562F23E0D9F143A4A9BF02060D37448527B5CDB59DBC1C05F5D826 iterator) {
    assertUnset.accept(iterator);
    assertFalse.accept(iterator.hasNext());
  }

  @Test
  void testConstructionAndBasicOperations() {
    // Test default constructor
    final var defaultIterator = iterDictEntryStrInt();
    assertNotNull(defaultIterator);
    assertIteratorUnset(defaultIterator);

    // Test value constructor
    final var valueIterator = iterDictEntryStrInt(testEntry());
    assertNotNull(valueIterator);
    assertIteratorSet(valueIterator);
  }

  @Test
  void testFactoryMethodsAndDelegation() {
    // Test _of() factory method - empty iterator
    final var emptyIterator = _Iterator_48D70134B2562F23E0D9F143A4A9BF02060D37448527B5CDB59DBC1C05F5D826._of();
    assertNotNull(emptyIterator);
    assertIteratorUnset(emptyIterator);

    // Test _of(null) factory method
    final var fromNull = iterDictEntryStrIntFromBase(null);
    assertNotNull(fromNull);
    assertIteratorUnset(fromNull);

    // Test _of(Iterator) factory method - from base Iterator with DictEntry
    final var entry = testEntry();
    final var baseIterator = Iterator._of(entry.getDelegate());
    final var fromBase = iterDictEntryStrIntFromBase(baseIterator);
    assertNotNull(fromBase);
    assertIteratorSet(fromBase);
  }

  @Test
  void testTypeSafetyAndIteration() {
    // Test iteration with DictEntry of (String, Integer) values
    final var entry = testEntry();
    final var iterator = iterDictEntryStrInt(entry);

    // Verify hasNext and next behavior with type safety
    assertIteratorSet(iterator);
    final var result = iterator.next();
    assertEquals(entry.key(), result.key());
    assertEquals(entry.value(), result.value());
    assertEquals(DICT_ENTRY_SI, result.getClass());
    assertEquals(String.class, result.key().getClass());
    assertEquals(Integer.class, result.value().getClass());

    // After consuming, should be empty
    assertIteratorUnset(iterator);
  }

  @Test
  void testOperatorsAndBehavior() {
    // Test operators with different states
    final var iterator1 = iterDictEntryStrInt();
    final var iterator2 = iterDictEntryStrInt();
    final var iteratorWithValue = iterDictEntryStrInt(testEntry());

    // Test equality functionality
    assertNotNull(iterator1._eq(iterator2));
    assertNotNull(iterator1._eq(iterator1));

    // Test _isSet operator (should return hasNext result)
    assertEquals(iteratorWithValue.hasNext(), iteratorWithValue._isSet());

    // Test _hashcode operator
    assertNotNull(iteratorWithValue._hashcode());
  }

}
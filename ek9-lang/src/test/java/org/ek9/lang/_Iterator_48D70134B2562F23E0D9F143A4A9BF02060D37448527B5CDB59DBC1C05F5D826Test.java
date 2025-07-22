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

  // Test data
  private final String testKey1 = String._of("key1");
  private final Integer testValue1 = Integer._of(10);

  @Test
  void testConstruction() {
    // Test default constructor
    final var defaultIterator = new _Iterator_48D70134B2562F23E0D9F143A4A9BF02060D37448527B5CDB59DBC1C05F5D826();
    assertNotNull(defaultIterator);
    assertUnset.accept(defaultIterator);
    assertFalse.accept(defaultIterator.hasNext());

    // Test value constructor
    final var testEntry =
        _DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E._of(testKey1, testValue1);
    final var valueIterator = new _Iterator_48D70134B2562F23E0D9F143A4A9BF02060D37448527B5CDB59DBC1C05F5D826(testEntry);
    assertNotNull(valueIterator);
    assertSet.accept(valueIterator);
    assertTrue.accept(valueIterator.hasNext());
  }

  @Test
  void testFactoryMethods() {
    // Test _of() - empty iterator
    final var emptyIterator = _Iterator_48D70134B2562F23E0D9F143A4A9BF02060D37448527B5CDB59DBC1C05F5D826._of();
    assertNotNull(emptyIterator);
    assertUnset.accept(emptyIterator);
    assertFalse.accept(emptyIterator.hasNext());

    // Test _of(Iterator) - from base Iterator with DictEntry
    final var testEntry =
        _DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E._of(testKey1, testValue1);
    final var baseIterator = Iterator._of(testEntry.getDelegate());

    final var fromBase = _Iterator_48D70134B2562F23E0D9F143A4A9BF02060D37448527B5CDB59DBC1C05F5D826._of(baseIterator);
    assertNotNull(fromBase);
    assertSet.accept(fromBase);
    assertTrue.accept(fromBase.hasNext());

    // Test _of(null Iterator)
    final var fromNull = _Iterator_48D70134B2562F23E0D9F143A4A9BF02060D37448527B5CDB59DBC1C05F5D826._of(null);
    assertNotNull(fromNull);
    assertUnset.accept(fromNull);
    assertFalse.accept(fromNull.hasNext());
  }

  @Test
  void testDictEntryIteration() {
    // Test iteration with DictEntry of (String, Integer) values
    final var testEntry =
        _DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E._of(testKey1, testValue1);
    final var iterator = new _Iterator_48D70134B2562F23E0D9F143A4A9BF02060D37448527B5CDB59DBC1C05F5D826(testEntry);

    // Verify hasNext and next behavior
    assertTrue.accept(iterator.hasNext());
    final var result = iterator.next();
    assertEquals(testEntry.key(), result.key());
    assertEquals(testEntry.value(), result.value());
    assertEquals(_DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E.class,
        result.getClass());

    // After consuming, should be empty
    assertFalse.accept(iterator.hasNext());
  }

  @Test
  void testEqualityOperator() {
    // Test _eq with same parameterized type
    final var iterator1 = _Iterator_48D70134B2562F23E0D9F143A4A9BF02060D37448527B5CDB59DBC1C05F5D826._of();
    final var iterator2 = _Iterator_48D70134B2562F23E0D9F143A4A9BF02060D37448527B5CDB59DBC1C05F5D826._of();

    // Test equality functionality
    assertNotNull(iterator1._eq(iterator2));
    assertNotNull(iterator1._eq(iterator1));

  }

  @Test
  void testOperatorConsistency() {
    final var entry =
        _DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E._of(testKey1, testValue1);
    final var iterator = new _Iterator_48D70134B2562F23E0D9F143A4A9BF02060D37448527B5CDB59DBC1C05F5D826(entry);

    // Test _isSet operator (should return hasNext result)
    assertEquals(iterator.hasNext(), iterator._isSet());

    // Test _hashcode operator
    assertNotNull(iterator._hashcode());
  }

  @Test
  void testNestedTypeSafety() {
    // Test that iteration returns correctly typed nested objects
    final var entry =
        _DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E._of(testKey1, testValue1);
    final var iterator = new _Iterator_48D70134B2562F23E0D9F143A4A9BF02060D37448527B5CDB59DBC1C05F5D826(entry);

    // Test that next() returns the correct nested parameterized type
    if (iterator.hasNext().state) {
      final var retrievedEntry = iterator.next();

      // Verify DictEntry type and nested key/value types
      assertEquals(_DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E.class,
          retrievedEntry.getClass());
      assertEquals(String.class, retrievedEntry.key().getClass());
      assertEquals(Integer.class, retrievedEntry.value().getClass());

      // Verify values
      assertEquals(testKey1, retrievedEntry.key());
      assertEquals(testValue1, retrievedEntry.value());
    }
  }
}
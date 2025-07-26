package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Test DictEntry of (String, Integer) parameterized type.
 * Tests dual-parameter delegation pattern and type safety for key-value pairs.
 */
@SuppressWarnings({"checkstyle:MethodName", "checkstyle:AbbreviationAsWordInName", "checkstyle:TypeName"})
class _DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4ETest extends Common {

  // Test data
  private final String testKey = String._of("testKey");
  private final Integer testValue = INT_42;
  private final String testKey2 = String._of("anotherKey");
  private final Integer testValue2 = Integer._of(99);

  @Test
  void testConstruction() {
    // Test default constructor
    final var defaultEntry = new _DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E();
    assertNotNull(defaultEntry);
    assertUnset.accept(defaultEntry);
    assertFalse.accept(defaultEntry._isSet());

    // Test two-parameter constructor
    final var valueEntry = new _DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E(testKey, testValue);
    assertNotNull(valueEntry);
    assertSet.accept(valueEntry);
    assertTrue.accept(valueEntry._isSet());
    assertEquals(testKey, valueEntry.key());
    assertEquals(testValue, valueEntry.value());
  }

  @Test
  void testFactoryMethods() {
    // Test _of() - empty entry
    final var emptyEntry = _DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E._of();
    assertNotNull(emptyEntry);
    assertUnset.accept(emptyEntry);
    assertFalse.accept(emptyEntry._isSet());

    // Test _of(String, Integer) - from key-value pair
    final var keyValueEntry = _DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E._of(testKey, testValue);
    assertNotNull(keyValueEntry);
    assertSet.accept(keyValueEntry);
    assertTrue.accept(keyValueEntry._isSet());
    assertEquals(testKey, keyValueEntry.key());
    assertEquals(testValue, keyValueEntry.value());

    // Test _of(DictEntry) - from base DictEntry
    final var baseDictEntry = DictEntry._of(testKey2, testValue2);
    final var fromBase = _DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E._of(baseDictEntry);
    assertNotNull(fromBase);
    assertSet.accept(fromBase);
    assertTrue.accept(fromBase._isSet());
    assertEquals(testKey2, fromBase.key());
    assertEquals(testValue2, fromBase.value());

    // Test _of(null DictEntry)
    final var fromNull = _DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E._of(null);
    assertNotNull(fromNull);
    assertUnset.accept(fromNull);
    assertFalse.accept(fromNull._isSet());
  }

  @Test
  void testKeyValueAccess() {
    // Test type-safe key and value access
    final var entry = _DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E._of(testKey, testValue);

    // Test key() returns String type
    final var key = entry.key();
    assertEquals(testKey, key);
    assertEquals(String.class, key.getClass());

    // Test value() returns Integer type
    final var value = entry.value();
    assertEquals(testValue, value);
    assertEquals(Integer.class, value.getClass());

    // Test with different key-value pairs
    final var entry2 = _DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E._of(STR_NAME, INT_100);
    assertEquals(STR_NAME, entry2.key());
    assertEquals(INT_100, entry2.value());
  }

  @Test
  void testEqualityOperators() {
    // Setup entries for testing
    final var entry1 = _DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E._of(testKey, testValue);
    assertNotNull(entry1);
    final var entry2 = _DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E._of(testKey, testValue);
    final var entry3 = _DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E._of(testKey2, testValue2);

    // Test _eq with same parameterized type
    assertTrue.accept(entry1._eq(entry2)); // Same key-value
    assertFalse.accept(entry1._eq(entry3)); // Different key-value
    assertUnset.accept(entry1._eq(null));

    // Test _eq with Any (polymorphic)
    assertTrue.accept(entry1._eq((Any) entry2));
    assertFalse.accept(entry1._eq((Any) entry3));

    // Test _neq operator
    assertFalse.accept(entry1._neq(entry2));
    assertTrue.accept(entry1._neq(entry3));
    assertUnset.accept(entry1._neq(null));
  }

  @Test
  void testComparisonOperators() {
    final var entry1 = _DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E._of(String._of("a"), Integer._of(1));
    final var entry2 = _DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E._of(String._of("a"), Integer._of(1));
    final var entry3 = _DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E._of(String._of("b"), Integer._of(2));

    // Test _cmp with same parameterized type
    final var cmpEqual = entry1._cmp(entry2);
    assertSet.accept(cmpEqual);
    assertEquals(Integer._of(0), cmpEqual); // Should be equal

    final var cmpDifferent = entry1._cmp(entry3);
    assertSet.accept(cmpDifferent);
    // Should be non-zero (exact value depends on String comparison)

    // Test _cmp with null
    assertUnset.accept(entry1._cmp(null));

    // Test _cmp with Any (polymorphic)
    final var cmpAny = entry1._cmp((Any) entry2);
    assertSet.accept(cmpAny);
    assertEquals(Integer._of(0), cmpAny);
  }

  @Test
  void testStringAndHashOperators() {
    final var entry = _DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E._of(testKey, testValue);

    // Test _string operator
    final var stringRep = entry._string();
    assertSet.accept(stringRep);
    assertNotNull(stringRep.state);

    // Test _hashcode operator
    final var hashcode = entry._hashcode();
    assertSet.accept(hashcode);

    // Test with unset entry
    final var unsetEntry = _DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E._of();
    assertUnset.accept(unsetEntry._string());
    assertUnset.accept(unsetEntry._hashcode());
  }

  @Test
  void testEdgeCases() {
    // Test with edge case key-value combinations
    final var emptyKey = String._of("");
    final var zeroValue = Integer._of(0);
    final var edgeEntry = _DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E._of(emptyKey, zeroValue);
    
    assertSet.accept(edgeEntry);
    assertEquals(emptyKey, edgeEntry.key());
    assertEquals(zeroValue, edgeEntry.value());

    // Test with large values
    final var longKey = String._of("VeryLongKeyNameForTesting");
    final var largeValue = Integer._of(1000000);
    final var largeEntry = _DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E._of(longKey, largeValue);
    
    assertSet.accept(largeEntry);
    assertEquals(longKey, largeEntry.key());
    assertEquals(largeValue, largeEntry.value());

    // Test with negative values
    final var negativeValue = Integer._of(-999);
    final var negativeEntry = _DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E._of(testKey, negativeValue);
    
    assertEquals(negativeValue, negativeEntry.value());
  }

  @Test
  void testTypeConsistencyWithBase() {
    // Create base DictEntry
    final var baseDictEntry = DictEntry._of(testKey, testValue);
    
    // Create parameterized entry from base
    final var paramEntry = _DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E._of(baseDictEntry);

    // Both should have consistent behavior
    assertEquals(baseDictEntry._isSet(), paramEntry._isSet());

    // Parameterized version should provide type-safe access
    assertEquals(testKey, paramEntry.key());
    assertEquals(String.class, paramEntry.key().getClass());
    assertEquals(testValue, paramEntry.value());
    assertEquals(Integer.class, paramEntry.value().getClass());
  }

  @Test
  void testDelegationBehavior() {
    // Test that parameterized DictEntry properly delegates to base DictEntry
    final var paramEntry = _DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E._of(testKey, testValue);
    final var baseEntry = DictEntry._of(testKey, testValue);

    // Verify delegation produces consistent behavior
    assertEquals(baseEntry._isSet(), paramEntry._isSet());
    assertEquals(baseEntry._string(), paramEntry._string());
    assertEquals(baseEntry._hashcode(), paramEntry._hashcode());
  }

  @Test
  void testTypeSafety() {
    final var entry = _DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E._of(testKey, testValue);

    // Verify class type is correct
    assertEquals(_DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E.class, 
                 entry.getClass());

    // Verify key and value types are correctly parameterized
    assertEquals(String.class, entry.key().getClass());
    assertEquals(Integer.class, entry.value().getClass());
  }
}
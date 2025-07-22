package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Test Dict of (String, Integer) parameterized type.
 * Tests complex two-parameter dictionary operations and integration with all dependent parameterized types.
 */
@SuppressWarnings({"checkstyle:MethodName", "checkstyle:AbbreviationAsWordInName", "checkstyle:TypeName"})
class _Dict_7E7710D38A91EC202D64601DF4D9FB5B4AF2026CC3EF59394F5CF6B738812BB6Test extends Common {

  // Test data
  private final String testKey1 = String._of("key1");
  private final Integer testValue1 = Integer._of(100);
  private final String testKey2 = String._of("key2");
  private final Integer testValue2 = Integer._of(200);
  private final String testKey3 = String._of("key3");
  private final Integer testValue3 = Integer._of(300);

  // Helper method to create a test dict with testKey1, testValue1
  private _Dict_7E7710D38A91EC202D64601DF4D9FB5B4AF2026CC3EF59394F5CF6B738812BB6 createTestDict() {
    return _Dict_7E7710D38A91EC202D64601DF4D9FB5B4AF2026CC3EF59394F5CF6B738812BB6._of(testKey1, testValue1);
  }

  @Test
  void testConstruction() {
    // Test default constructor
    final var defaultDict = new _Dict_7E7710D38A91EC202D64601DF4D9FB5B4AF2026CC3EF59394F5CF6B738812BB6();
    assertNotNull(defaultDict);
    assertSet.accept(defaultDict);
    assertTrue.accept(defaultDict._empty());

    // Test two-parameter constructor
    final var keyValueDict =
        new _Dict_7E7710D38A91EC202D64601DF4D9FB5B4AF2026CC3EF59394F5CF6B738812BB6(testKey1, testValue1);
    assertNotNull(keyValueDict);
    assertSet.accept(keyValueDict);
    assertFalse.accept(keyValueDict._empty());
    assertEquals(testValue1, keyValueDict.get(testKey1));
  }

  @Test
  void testFactoryMethods() {
    // Test _of() - empty dict
    final var emptyDict = _Dict_7E7710D38A91EC202D64601DF4D9FB5B4AF2026CC3EF59394F5CF6B738812BB6._of();
    assertNotNull(emptyDict);
    assertSet.accept(emptyDict);
    assertTrue.accept(emptyDict._empty());

    // Test _of(String, Integer) - from key-value pair
    final var keyValueDict =
        _Dict_7E7710D38A91EC202D64601DF4D9FB5B4AF2026CC3EF59394F5CF6B738812BB6._of(testKey1, testValue1);
    assertNotNull(keyValueDict);
    assertSet.accept(keyValueDict);
    assertFalse.accept(keyValueDict._empty());
    assertEquals(testValue1, keyValueDict.get(testKey1));

    // Test _of(Dict) - from base Dict
    final var baseDict = Dict._of(testKey2, testValue2);
    final var fromBase = _Dict_7E7710D38A91EC202D64601DF4D9FB5B4AF2026CC3EF59394F5CF6B738812BB6._of(baseDict);
    assertNotNull(fromBase);
    assertSet.accept(fromBase);
    assertEquals(testValue2, fromBase.get(testKey2));

    // Test _of(null Dict)
    final var fromNull = _Dict_7E7710D38A91EC202D64601DF4D9FB5B4AF2026CC3EF59394F5CF6B738812BB6._of(null);
    assertNotNull(fromNull);
    assertSet.accept(fromNull);
    assertTrue.accept(fromNull._empty());
  }

  @Test
  void testGetOperations() {
    final var dict = _Dict_7E7710D38A91EC202D64601DF4D9FB5B4AF2026CC3EF59394F5CF6B738812BB6._of(testKey1, testValue1);

    // Test get() with existing key
    final var value = dict.get(testKey1);
    assertEquals(testValue1, value);
    assertEquals(Integer.class, value.getClass());

    // Test get() with non-existing key - should throw exception
    assertThrows(Exception.class, () -> dict.get(String._of("nonexistent")));

    // Test getOrDefault() with existing key
    final var existingValue = dict.getOrDefault(testKey1, testValue2);
    assertEquals(testValue1, existingValue);

    // Test getOrDefault() with non-existing key
    final var defaultValue = dict.getOrDefault(String._of("missing"), testValue3);
    assertEquals(testValue3, defaultValue);
  }

  @Test
  void testIteratorMethodsAndTypeSafety() {
    // Create dict with multiple entries for comprehensive testing
    final var dict = createTestDict();
    dict._pipe(_DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E._of(testKey2, testValue2));

    // Verify class type is correct
    assertEquals(_Dict_7E7710D38A91EC202D64601DF4D9FB5B4AF2026CC3EF59394F5CF6B738812BB6.class, 
                 dict.getClass());

    // Verify get returns correctly typed objects
    assertEquals(String.class, testKey1.getClass());
    assertEquals(Integer.class, dict.get(testKey1).getClass());

    // Test iterator() - returns Iterator of DictEntry of (String, Integer)
    final var entryIterator = dict.iterator();
    assertNotNull(entryIterator);
    assertEquals(_Iterator_48D70134B2562F23E0D9F143A4A9BF02060D37448527B5CDB59DBC1C05F5D826.class,
        entryIterator.getClass());
    assertTrue.accept(entryIterator.hasNext());

    // Test keys() - returns Iterator of String
    final var keyIterator = dict.keys();
    assertNotNull(keyIterator);
    assertEquals(_Iterator_852BE8F78E9C7E622E0E2BDC5523BEFF664305AD702B04CE0463ED42C1FE2CA2.class,
        keyIterator.getClass());
    assertTrue.accept(keyIterator.hasNext());

    // Test values() - returns Iterator of Integer
    final var valueIterator = dict.values();
    assertNotNull(valueIterator);
    assertEquals(_Iterator_2648BF49C605A31A24BDA0751F2E0F0936F554C621EB0CE8472863EE4EEB4EB4.class,
        valueIterator.getClass());
    assertTrue.accept(valueIterator.hasNext());
  }

  @Test
  void testEqualityOperators() {
    final var dict1 = _Dict_7E7710D38A91EC202D64601DF4D9FB5B4AF2026CC3EF59394F5CF6B738812BB6._of(testKey1, testValue1);
    assertNotNull(dict1);
    final var dict2 = _Dict_7E7710D38A91EC202D64601DF4D9FB5B4AF2026CC3EF59394F5CF6B738812BB6._of(testKey1, testValue1);
    final var dict3 = _Dict_7E7710D38A91EC202D64601DF4D9FB5B4AF2026CC3EF59394F5CF6B738812BB6._of(testKey2, testValue2);

    // Test _eq with same parameterized type
    assertTrue.accept(dict1._eq(dict2)); // Same content
    assertFalse.accept(dict1._eq(dict3)); // Different content
    assertUnset.accept(dict1._eq(null));

    // Test _eq with Any (polymorphic)
    assertTrue.accept(dict1._eq((Any) dict2));
    assertFalse.accept(dict1._eq((Any) dict3));

    // Test _neq operator
    assertFalse.accept(dict1._neq(dict2));
    assertTrue.accept(dict1._neq(dict3));
    assertUnset.accept(dict1._neq(null));
  }

  @Test
  void testArithmeticOperators() {
    final var dict1 = _Dict_7E7710D38A91EC202D64601DF4D9FB5B4AF2026CC3EF59394F5CF6B738812BB6._of(testKey1, testValue1);
    final var dict2 = _Dict_7E7710D38A91EC202D64601DF4D9FB5B4AF2026CC3EF59394F5CF6B738812BB6._of(testKey2, testValue2);

    // Test + operator with same parameterized type
    final var combined = dict1._add(dict2);
    assertNotNull(combined);
    assertEquals(_Dict_7E7710D38A91EC202D64601DF4D9FB5B4AF2026CC3EF59394F5CF6B738812BB6.class,
        combined.getClass());
    assertEquals(testValue1, combined.get(testKey1));
    assertEquals(testValue2, combined.get(testKey2));

    // Test + operator with DictEntry of (String, Integer)
    final var entry =
        _DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E._of(testKey3, testValue3);
    final var withEntry = dict1._add(entry);
    assertNotNull(withEntry);
    assertEquals(testValue1, withEntry.get(testKey1));
    assertEquals(testValue3, withEntry.get(testKey3));

    // Test - operator with same parameterized type
    final var subtracted = combined._sub(dict2);
    assertNotNull(subtracted);
    assertEquals(testValue1, subtracted.get(testKey1));
    // Test that key2 was removed - should throw exception
    assertThrows(Exception.class, () -> subtracted.get(testKey2));

    // Test + operator with null
    final var addedNull = dict1._add((_Dict_7E7710D38A91EC202D64601DF4D9FB5B4AF2026CC3EF59394F5CF6B738812BB6) null);
    assertNotNull(addedNull);
  }

  @Test
  void testSizeAndEmptyOperators() {
    final var emptyDict = _Dict_7E7710D38A91EC202D64601DF4D9FB5B4AF2026CC3EF59394F5CF6B738812BB6._of();
    final var dict = _Dict_7E7710D38A91EC202D64601DF4D9FB5B4AF2026CC3EF59394F5CF6B738812BB6._of(testKey1, testValue1);

    // Test _empty operator
    assertTrue.accept(emptyDict._empty());
    assertFalse.accept(dict._empty());

    // Test _len operator
    assertEquals(Integer._of(0), emptyDict._len());
    assertEquals(Integer._of(1), dict._len());

    // Test _contains operator
    assertFalse.accept(dict._contains(String._of("missing")));
    assertTrue.accept(dict._contains(testKey1));
  }

  @Test
  void testStringAndHashOperators() {
    final var dict = _Dict_7E7710D38A91EC202D64601DF4D9FB5B4AF2026CC3EF59394F5CF6B738812BB6._of(testKey1, testValue1);

    // Test _string operator
    final var stringRep = dict._string();
    assertSet.accept(stringRep);
    assertNotNull(stringRep.state);

    // Test _hashcode operator
    final var hashcode = dict._hashcode();
    assertSet.accept(hashcode);

    // Test _isSet operator
    assertTrue.accept(dict._isSet());
  }

  @Test
  void testAssignmentOperators() {
    final var sourceDict =
        _Dict_7E7710D38A91EC202D64601DF4D9FB5B4AF2026CC3EF59394F5CF6B738812BB6._of(testKey1, testValue1);
    final var targetDict = _Dict_7E7710D38A91EC202D64601DF4D9FB5B4AF2026CC3EF59394F5CF6B738812BB6._of();

    // Test _copy operator
    targetDict._copy(sourceDict);
    assertEquals(testValue1, targetDict.get(testKey1));

    // Test _replace operator (should behave like copy)
    final var replaceTarget =
        _Dict_7E7710D38A91EC202D64601DF4D9FB5B4AF2026CC3EF59394F5CF6B738812BB6._of(testKey2, testValue2);
    replaceTarget._replace(sourceDict);
    assertEquals(testValue1, replaceTarget.get(testKey1));
    // Test that original key2 was replaced - should throw exception
    assertThrows(Exception.class, () -> replaceTarget.get(testKey2));

    // Test _merge operator
    final var mergeTarget =
        _Dict_7E7710D38A91EC202D64601DF4D9FB5B4AF2026CC3EF59394F5CF6B738812BB6._of(testKey2, testValue2);
    mergeTarget._merge(sourceDict);
    assertEquals(testValue1, mergeTarget.get(testKey1)); // Added
    assertEquals(testValue2, mergeTarget.get(testKey2)); // Kept

    // Test with null
    final var nullTarget = _Dict_7E7710D38A91EC202D64601DF4D9FB5B4AF2026CC3EF59394F5CF6B738812BB6._of();
    nullTarget._copy(null); // Should not crash
    nullTarget._replace(null); // Should not crash
    nullTarget._merge(null); // Should not crash
  }

  @Test
  void testPipeAndAddAssOperators() {
    final var dict = _Dict_7E7710D38A91EC202D64601DF4D9FB5B4AF2026CC3EF59394F5CF6B738812BB6._of();

    // Test _pipe with DictEntry of (String, Integer)
    final var entry =
        _DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E._of(testKey1, testValue1);
    dict._pipe(entry);
    assertEquals(testValue1, dict.get(testKey1));

    // Test _addAss with same parameterized type
    final var otherDict =
        _Dict_7E7710D38A91EC202D64601DF4D9FB5B4AF2026CC3EF59394F5CF6B738812BB6._of(testKey2, testValue2);
    dict._addAss(otherDict);
    assertEquals(testValue1, dict.get(testKey1)); // Keep existing
    assertEquals(testValue2, dict.get(testKey2)); // Add new

    // Test _addAss with DictEntry of (String, Integer)
    final var entry3 =
        _DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E._of(testKey3, testValue3);
    dict._addAss(entry3);
    assertEquals(testValue3, dict.get(testKey3));

    // Test _subAss with String key
    dict._subAss(testKey2);
    // Test that key2 was removed - should throw exception
    assertThrows(Exception.class, () -> dict.get(testKey2));

    // Test with null
    dict._addAss((_Dict_7E7710D38A91EC202D64601DF4D9FB5B4AF2026CC3EF59394F5CF6B738812BB6) null);
  }

  @Test
  void testComplexDictionaryOperations() {
    // Test complex scenario with multiple operations and entries
    final var dict = _Dict_7E7710D38A91EC202D64601DF4D9FB5B4AF2026CC3EF59394F5CF6B738812BB6._of();

    // Add entries using different methods to test comprehensive functionality
    dict._pipe(_DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E._of(String._of("name"),
        Integer._of(100)));
    dict._pipe(_DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E._of(String._of("age"),
        Integer._of(25)));
    dict._pipe(_DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E._of(String._of("score"),
        Integer._of(95)));

    // Verify all entries exist and are correctly typed
    assertEquals(Integer._of(100), dict.get(String._of("name")));
    assertEquals(Integer._of(25), dict.get(String._of("age")));
    assertEquals(Integer._of(95), dict.get(String._of("score")));
    assertEquals(Integer._of(3), dict._len());

    // Test that all iterators work with multiple entries (functionality tested in detail elsewhere)
    assertTrue.accept(dict.iterator().hasNext());
    assertTrue.accept(dict.keys().hasNext());
    assertTrue.accept(dict.values().hasNext());
  }

  @Test
  void testDelegationAndConsistencyWithBase() {
    // Create base Dict and parameterized dict for comprehensive consistency testing
    final var baseDict = Dict._of(testKey1, testValue1);
    final var paramDict = _Dict_7E7710D38A91EC202D64601DF4D9FB5B4AF2026CC3EF59394F5CF6B738812BB6._of(baseDict);
    final var directParamDict = createTestDict();

    // Test consistency between base and parameterized created from base
    assertEquals(baseDict._isSet(), paramDict._isSet());
    assertEquals(baseDict._empty(), paramDict._empty());
    assertEquals(baseDict._len(), paramDict._len());

    // Parameterized version should provide type-safe access
    assertEquals(testValue1, paramDict.get(testKey1));
    assertEquals(Integer.class, paramDict.get(testKey1).getClass());

    // Test delegation behavior between equivalent dicts
    assertEquals(baseDict._isSet(), directParamDict._isSet());
    assertEquals(baseDict._empty(), directParamDict._empty());
    assertEquals(baseDict._len(), directParamDict._len());
    assertEquals(baseDict._string(), directParamDict._string());
    assertEquals(baseDict._hashcode(), directParamDict._hashcode());
  }


  @Test
  void testEdgeCases() {
    // Test with edge case key-value combinations
    final var emptyKey = String._of("");
    final var zeroValue = Integer._of(0);
    final var edgeDict =
        _Dict_7E7710D38A91EC202D64601DF4D9FB5B4AF2026CC3EF59394F5CF6B738812BB6._of(emptyKey, zeroValue);

    assertSet.accept(edgeDict);
    assertEquals(zeroValue, edgeDict.get(emptyKey));

    // Test with large values
    final var longKey = String._of("VeryLongKeyNameForTesting");
    final var largeValue = Integer._of(999999999);
    final var largeDict =
        _Dict_7E7710D38A91EC202D64601DF4D9FB5B4AF2026CC3EF59394F5CF6B738812BB6._of(longKey, largeValue);

    assertEquals(largeValue, largeDict.get(longKey));

    // Test with negative values
    final var negativeValue = Integer._of(-999);
    final var negativeDict =
        _Dict_7E7710D38A91EC202D64601DF4D9FB5B4AF2026CC3EF59394F5CF6B738812BB6._of(testKey1, negativeValue);

    assertEquals(negativeValue, negativeDict.get(testKey1));

    // Test overriding values
    final var dict = _Dict_7E7710D38A91EC202D64601DF4D9FB5B4AF2026CC3EF59394F5CF6B738812BB6._of(testKey1, testValue1);
    dict._pipe(_DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E._of(testKey1, testValue2));
    assertEquals(testValue2, dict.get(testKey1)); // Should be overridden
  }
}
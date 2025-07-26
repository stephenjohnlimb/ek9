package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.LinkedHashMap;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DictTest extends Common {

  // Test data
  final String key1 = String._of("firstName");
  final String key2 = String._of("lastName");
  final String key3 = String._of("age");
  final String key4 = String._of("city");
  final String value1 = String._of("John");
  final String value2 = String._of("Doe");
  final Integer value3 = Integer._of(25);
  final String value4 = String._of("NYC");

  final Dict emptyDict = new Dict();

  // Helper methods to reduce duplication

  /**
   * Creates a standard 3-entry dict with key1->value1, key2->value2, key3->value3
   */
  private Dict createThreeEntryDict() {
    final var dict = new Dict(key1, value1);
    dict._addAss(DictEntry._of(key2, value2));
    dict._addAss(DictEntry._of(key3, value3));
    return dict;
  }

  /**
   * Creates a standard 2-entry dict with key1->value1, key2->value2
   */
  private Dict createTwoEntryDict() {
    final var dict = new Dict(key1, value1);
    dict._addAss(DictEntry._of(key2, value2));
    return dict;
  }

  /**
   * Collects all items from an iterator into an ArrayList
   */
  private <T> java.util.ArrayList<T> collectIterator(Iterator iterator, Class<T> type) {
    final var items = new java.util.ArrayList<T>();
    while (iterator.hasNext().state) {
      items.add(type.cast(iterator.next()));
    }
    return items;
  }

  /**
   * Asserts that an operation doesn't change the dict size
   */
  private void assertNoSizeChange(Dict dict, Runnable operation) {
    final var sizeBefore = dict._len().state;
    operation.run();
    assertEquals(sizeBefore, dict._len().state);
  }

  /**
   * Creates two identical dicts for equality testing
   */
  private Dict[] createEqualDictPair() {
    final var dict1 = createTwoEntryDict();
    final var dict2 = createTwoEntryDict();
    return new Dict[]{dict1, dict2};
  }

  @Nested
  class Construction {
    
    @Test
    void testConstruction() {
      // Default constructor creates empty but set dict
      final var defaultConstructor = new Dict();
      assertSet.accept(defaultConstructor);
      assertTrue.accept(defaultConstructor._empty());
      assertEquals(0L, defaultConstructor._len().state);

      // Constructor with key-value pair
      final var keyValueDict = new Dict(key1, value1);
      assertSet.accept(keyValueDict);
      assertFalse.accept(keyValueDict._empty());
      assertEquals(1L, keyValueDict._len().state);
      assertEquals(value1, keyValueDict.get(key1));

      // Constructor with null key should create empty dict
      final var nullKeyDict = new Dict(null, value1);
      assertSet.accept(nullKeyDict);
      assertTrue.accept(nullKeyDict._empty());

      // Constructor with null value should create empty dict
      final var nullValueDict = new Dict(key1, null);
      assertSet.accept(nullValueDict);
      assertTrue.accept(nullValueDict._empty());

      // Static factory methods
      final var factoryEmpty = Dict._of();
      assertSet.accept(factoryEmpty);
      assertTrue.accept(factoryEmpty._empty());

      final var factoryWithPair = Dict._of(key1, value1);
      assertSet.accept(factoryWithPair);
      assertEquals(1L, factoryWithPair._len().state);
      assertEquals(value1, factoryWithPair.get(key1));

      // Factory with Java map
      final var javaMap = new LinkedHashMap<Any, Any>();
      javaMap.put(key1, value1);
      javaMap.put(key2, value2);
      final var factoryFromMap = Dict._of(javaMap);
      assertSet.accept(factoryFromMap);
      assertEquals(2L, factoryFromMap._len().state);
      assertEquals(value1, factoryFromMap.get(key1));
      assertEquals(value2, factoryFromMap.get(key2));

      // Factory with null map
      final var nullMapDict = Dict._of(null);
      assertSet.accept(nullMapDict);
      assertTrue.accept(nullMapDict._empty());
    }

    @Test
    void testIsSet() {
      assertNotNull(emptyDict);
      assertTrue.accept(emptyDict._isSet());

      final var populatedDict = new Dict(key1, value1);
      assertNotNull(populatedDict);
      assertTrue.accept(populatedDict._isSet());

      // Dict is always set, even when empty
      assertTrue.accept(emptyDict._isSet());
    }
  }

  @Nested
  class ElementAccess {
    
    @Test
    void testGetMethod() {
      final var testDict = createThreeEntryDict();

      // Valid keys
      assertEquals(value1, testDict.get(key1));
      assertEquals(value2, testDict.get(key2));
      assertEquals(value3, testDict.get(key3));

      // Invalid key - now throws exception
      assertThrows(Exception.class, () -> testDict.get(key4));

      // Null key - now throws exception
      assertThrows(Exception.class, () -> testDict.get(null));

      // Unset key (should handle gracefully) - now throws exception
      final var unsetKey = new String();
      assertThrows(Exception.class, () -> testDict.get(unsetKey));
    }

    @Test
    void testGetOrDefaultMethod() {
      final var testDict = new Dict(key1, value1);

      // Existing key should return actual value
      assertEquals(value1, testDict.getOrDefault(key1, value2));

      // Non-existing key should return default
      assertEquals(value2, testDict.getOrDefault(key2, value2));

      // Null key should return default
      assertEquals(value3, testDict.getOrDefault(null, value3));

      // Test with unset key
      final var unsetKey = new String();
      assertEquals(value4, testDict.getOrDefault(unsetKey, value4));
    }
  }

  @Nested
  class IterationOperations {
    
    @Test
    void testIterator() {
      // Empty dict iterator
      final var emptyIterator = emptyDict.iterator();
      assertNotNull(emptyIterator);
      assertUnset.accept(emptyIterator);
      assertFalse.accept(emptyIterator.hasNext());

      // Single entry iterator
      final var singleDict = new Dict(key1, value1);
      final var singleIterator = singleDict.iterator();
      assertSet.accept(singleIterator);
      assertTrue.accept(singleIterator.hasNext());

      final var entry = singleIterator.next();
      if(entry instanceof DictEntry dictEntry) {
        assertEquals(key1, dictEntry.key());
        assertEquals(value1, dictEntry.value());
      } else {
        fail("Expecting type of DictEntry");
      }
      assertFalse.accept(singleIterator.hasNext());

      // Multiple entry iterator
      final var multiDict = createThreeEntryDict();
      final var multiIterator = multiDict.iterator();

      // Collect all entries
      final var entries = collectIterator(multiIterator, DictEntry.class);

      assertEquals(3, entries.size());
      // Should maintain insertion order (LinkedHashMap)
      assertEquals(key1, entries.get(0).key());
      assertEquals(key2, entries.get(1).key());
      assertEquals(key3, entries.get(2).key());
    }

    @Test
    void testKeysAndValues() {
      final var testDict = createThreeEntryDict();

      // Test keys iterator
      final var keysIterator = testDict.keys();
      assertSet.accept(keysIterator);

      final var keysList = collectIterator(keysIterator, Any.class);
      assertEquals(3, keysList.size());
      assertTrue.accept(Boolean._of(keysList.contains(key1)));
      assertTrue.accept(Boolean._of(keysList.contains(key2)));
      assertTrue.accept(Boolean._of(keysList.contains(key3)));

      // Test values iterator
      final var valuesIterator = testDict.values();
      assertSet.accept(valuesIterator);

      final var valuesList = collectIterator(valuesIterator, Any.class);
      assertEquals(3, valuesList.size());
      assertTrue.accept(Boolean._of(valuesList.contains(value1)));
      assertTrue.accept(Boolean._of(valuesList.contains(value2)));
      assertTrue.accept(Boolean._of(valuesList.contains(value3)));

      // Empty dict keys and values
      final var emptyKeys = emptyDict.keys();
      final var emptyValues = emptyDict.values();
      assertUnset.accept(emptyKeys);
      assertUnset.accept(emptyValues);
    }
  }

  @Nested
  class ComparisonOperators {
    
    @Test
    void testEquality() {
      final var equalPair = createEqualDictPair();
      final var dict1 = equalPair[0];
      final var dict2 = equalPair[1];
      final var dict3 = new Dict(key1, value1);
      dict3._addAss(DictEntry._of(key3, value3));

      // Equality
      assertTrue.accept(dict1._eq(dict2));
      assertFalse.accept(dict1._eq(dict3));
      assertFalse.accept(emptyDict._eq(dict1));
      assertTrue.accept(emptyDict._eq(new Dict()));

      // Inequality
      assertFalse.accept(dict1._neq(dict2));
      assertTrue.accept(dict1._neq(dict3));
      assertTrue.accept(emptyDict._neq(dict1));
      assertFalse.accept(emptyDict._neq(new Dict()));

      // Java equals
      assertEquals(dict1, dict2);
      assertNotEquals(dict1, dict3);
      assertEquals(dict1.hashCode(), dict2.hashCode());
    }
  }

  @Nested
  class ImmutableArithmeticOperations {
    
    @Test
    void testImmutableAddition() {
      final var dict1 = new Dict(key1, value1);
      final var dict2 = new Dict(key2, value2);
      dict2._addAss(DictEntry._of(key3, value3));

      // Add dict to dict (immutable)
      final var addedDict = dict1._add(dict2);
      assertEquals(3L, addedDict._len().state);
      assertEquals(value1, addedDict.get(key1));
      assertEquals(value2, addedDict.get(key2));
      assertEquals(value3, addedDict.get(key3));

      // Original dicts unchanged
      assertEquals(1L, dict1._len().state);
      assertEquals(2L, dict2._len().state);

      // Add DictEntry to dict (immutable)
      final var entry = DictEntry._of(key4, value4);
      final var addedEntry = dict1._add(entry);
      assertEquals(2L, addedEntry._len().state);
      assertEquals(value1, addedEntry.get(key1));
      assertEquals(value4, addedEntry.get(key4));

      // Original dict unchanged
      assertEquals(1L, dict1._len().state);

      // Add with empty dict
      final var addedEmpty = dict1._add(emptyDict);
      assertEquals(1L, addedEmpty._len().state);
      assertEquals(value1, addedEmpty.get(key1));

      // Add with null dict should not crash
      final var addedNull = dict1._add((Dict) null);
      assertSet.accept(addedNull);
      assertEquals(dict1, addedNull);
    }

    @Test
    void testImmutableSubtraction() {
      final var dict1 = createThreeEntryDict();
      final var dict2 = new Dict(key2, value2);
      dict2._addAss(DictEntry._of(key4, value4)); // key4 not in dict1

      // Subtract dict from dict (immutable)
      final var subtractedDict = dict1._sub(dict2);
      assertEquals(2L, subtractedDict._len().state);
      assertEquals(value1, subtractedDict.get(key1));
      assertEquals(value3, subtractedDict.get(key3));
      // Should be removed - now throws exception
      assertThrows(Exception.class, () -> subtractedDict.get(key2));
      // Wasn't there anyway - now throws exception
      assertThrows(Exception.class, () -> subtractedDict.get(key4));

      // Original dicts unchanged
      assertEquals(3L, dict1._len().state);
      assertEquals(2L, dict2._len().state);

      // Subtract with empty dict
      final var subtractedEmpty = dict1._sub(emptyDict);
      assertEquals(3L, subtractedEmpty._len().state);

      // Subtract with null dict should not crash
      final var subtractedNull = dict1._sub(null);
      assertSet.accept(subtractedNull);
      assertEquals(dict1, subtractedNull);
    }
  }

  @Nested
  class MutableArithmeticOperations {
    
    @Test
    void testMutableAddition() {
      final var dict1 = new Dict(key1, value1);
      final var dict2 = new Dict(key2, value2);
      dict2._addAss(DictEntry._of(key3, value3));

      // Add dict to dict (mutable)
      final var originalSize = dict1._len().state;
      dict1._addAss(dict2);
      assertEquals(originalSize + 2, dict1._len().state);
      assertEquals(value1, dict1.get(key1));
      assertEquals(value2, dict1.get(key2));
      assertEquals(value3, dict1.get(key3));

      // Add DictEntry to dict (mutable)
      final var entry = DictEntry._of(key4, value4);
      dict1._addAss(entry);
      assertEquals(4L, dict1._len().state);
      assertEquals(value4, dict1.get(key4));

      // Add with empty dict should do nothing
      assertNoSizeChange(dict1, () -> dict1._addAss(emptyDict));

      // Add with null should do nothing
      assertNoSizeChange(dict1, () -> dict1._addAss((Dict) null));
    }

    @Test
    void testMutableSubtraction() {
      final var dict1 = createThreeEntryDict();

      // Subtract key from dict (mutable)
      dict1._subAss(key2);
      assertEquals(2L, dict1._len().state);
      assertEquals(value1, dict1.get(key1));
      assertEquals(value3, dict1.get(key3));
      // key2 was removed - now throws exception
      assertThrows(Exception.class, () -> dict1.get(key2));

      // Subtract non-existent key should do nothing
      assertNoSizeChange(dict1, () -> dict1._subAss(key4));

      // Subtract with null key should do nothing
      assertNoSizeChange(dict1, () -> dict1._subAss(null));
    }
  }

  @Nested
  class AssignmentOperators {
    
    @Test
    void testAssignmentOperators() {
      // Test copy (:=:)
      final var original = createTwoEntryDict();

      final var copy = new Dict();
      copy._copy(original);

      assertTrue.accept(original._eq(copy));
      assertEquals(original._len().state, copy._len().state);

      // Test replace (:^:)
      final var replace = new Dict(key3, value3);
      replace._replace(original);
      assertTrue.accept(original._eq(replace));

      // Test merge (:~:)
      final var dict1 = new Dict(key1, value1);
      final var dict2 = new Dict(key2, value2);
      dict2._addAss(DictEntry._of(key3, value3));

      dict1._merge(dict2);
      assertEquals(3L, dict1._len().state);
      assertEquals(value1, dict1.get(key1));
      assertEquals(value2, dict1.get(key2));
      assertEquals(value3, dict1.get(key3));
    }

    @Test
    void testPipelineOperator() {
      final var dict = new Dict();

      // Pipe entries into dict
      final var entry1 = DictEntry._of(key1, value1);
      final var entry2 = DictEntry._of(key2, value2);
      final var entry3 = DictEntry._of(key3, value3);

      dict._pipe(entry1);
      dict._pipe(entry2);
      dict._pipe(entry3);

      assertEquals(3L, dict._len().state);
      assertEquals(value1, dict.get(key1));
      assertEquals(value2, dict.get(key2));
      assertEquals(value3, dict.get(key3));

      // Pipe with null entry should do nothing
      assertNoSizeChange(dict, () -> dict._pipe(null));
    }
  }

  @Nested
  class UtilityOperators {
    
    @Test
    void testUtilityOperators() {
      final var testDict = createTwoEntryDict();

      // Empty check
      assertFalse.accept(testDict._empty());
      assertTrue.accept(emptyDict._empty());

      // Length
      assertEquals(2L, testDict._len().state);
      assertEquals(0L, emptyDict._len().state);

      // Contains
      assertTrue.accept(testDict._contains(key1));
      assertTrue.accept(testDict._contains(key2));
      assertFalse.accept(testDict._contains(key3));
      assertUnset.accept(testDict._contains(null));

      // Hash code
      assertSet.accept(testDict._hashcode());
      assertSet.accept(emptyDict._hashcode());

      // String conversion
      assertSet.accept(testDict._string());
      final var stringRep = testDict.toString();
      assertNotNull(stringRep);
      assertTrue.accept(Boolean._of(stringRep.contains(key1.toString())));
      assertTrue.accept(Boolean._of(stringRep.contains(value1.toString())));

      // Empty dict string
      final var emptyString = emptyDict.toString();
      assertEquals("{}", emptyString);
    }

    @Test
    void testKeyValueOverwrites() {
      final var dict = new Dict(key1, value1);
      assertEquals(value1, dict.get(key1));

      // Adding same key with different value should overwrite
      dict._addAss(DictEntry._of(key1, value2));
      assertEquals(1L, dict._len().state); // Still only one entry
      assertEquals(value2, dict.get(key1)); // But value changed

      // Test with immutable addition
      final var dict2 = new Dict(key1, value1);
      final var entry = DictEntry._of(key1, value3);
      final var resultDict = dict2._add(entry);
      assertEquals(1L, resultDict._len().state);
      assertEquals(value3, resultDict.get(key1));
    }
  }

  @Nested
  class EdgeCases {
    
    @Test
    void testEdgeCases() {
      // Large dict operations
      final var largeDict = new Dict();
      for (int i = 0; i < 100; i++) {
        final var key = String._of("key" + i);
        final var value = Integer._of(i);
        largeDict._addAss(DictEntry._of(key, value));
      }
      assertEquals(100L, largeDict._len().state);

      // Operations on empty dict
      assertTrue.accept(emptyDict._empty());
      assertEquals(0L, emptyDict._len().state);
      assertFalse.accept(emptyDict._contains(key1));

      // Mixed type dict (all inherit from Any)
      final var mixedDict = new Dict(key1, value1);
      mixedDict._addAss(DictEntry._of(key3, value3)); // String key, Integer value
      mixedDict._addAss(DictEntry._of(value3, key1)); // Integer key, String value
      assertEquals(3L, mixedDict._len().state);

      // String representation with different types
      final var stringDictRep = mixedDict.toString();
      assertNotNull(stringDictRep);
      assertTrue.accept(Boolean._of(stringDictRep.contains(key1.toString())));
    }

    @Test
    void testDictEntryIntegration() {
      // Test seamless integration between Dict and DictEntry
      final var entry1 = DictEntry._of(key1, value1);
      final var entry2 = DictEntry._of(key2, value2);

      // Create dict from entries
      final var dict = new Dict();
      dict._addAss(entry1);
      dict._addAss(entry2);

      // Iterate and verify entries
      final var iterator = dict.iterator();
      final var entries = collectIterator(iterator, DictEntry.class);

      assertEquals(2, entries.size());

      // Verify entry equality
      boolean foundEntry1 = false;
      boolean foundEntry2 = false;
      for (final var entry : entries) {
        if (entry1._eq(entry).state) {
          foundEntry1 = true;
        }
        if (entry2._eq(entry).state) {
          foundEntry2 = true;
        }
      }
      assertTrue.accept(Boolean._of(foundEntry1));
      assertTrue.accept(Boolean._of(foundEntry2));
    }

    @Test
    void testConstraintValidation() {
      // Test that constraint validation works properly
      final var javaMap = new LinkedHashMap<Any, Any>();
      javaMap.put(key1, value1);
      javaMap.put(key2, value2);

      // This should work fine as basic dicts don't have constraints
      final var dict = Dict._of(javaMap);
      assertSet.accept(dict);
      assertEquals(2L, dict._len().state);
    }

    @Test
    void testEqualsAndHashCode() {
      // Test equals contract
      final var equalPair = createEqualDictPair();
      final var dict1 = equalPair[0];
      final var dict2 = equalPair[1];

      assertEquals(dict1, dict1); // reflexive
      assertEquals(dict1, dict2); // symmetric
      assertEquals(dict2, dict1); // symmetric
      assertNotEquals(null, dict1);
      assertNotEquals("not a dict", dict1);

      // Unset dicts are equal
      assertEquals(new Dict(), new Dict());

      // Hash code consistency - equal objects should have equal hash codes
      if (dict1.equals(dict2)) {
        assertEquals(dict1.hashCode(), dict2.hashCode());
      }

      // Different dicts have different hash codes
      final var dict3 = new Dict(key3, value3);
      assertNotEquals(dict1.hashCode(), dict3.hashCode());
    }
  }
}
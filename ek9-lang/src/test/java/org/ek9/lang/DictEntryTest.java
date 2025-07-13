package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class DictEntryTest extends Common {

  final String key1 = String._of("firstName");
  final String key2 = String._of("lastName");
  final String key3 = String._of("age");
  final String value1 = String._of("John");
  final String value2 = String._of("Doe");
  final Integer value3 = Integer._of(25);

  @Test
  void testConstruction() {
    // Default constructor creates unset entry
    final var defaultConstructor = new DictEntry();
    assertUnset.accept(defaultConstructor);
    assertFalse.accept(defaultConstructor.key()._isSet());
    assertFalse.accept(defaultConstructor.value()._isSet());

    // Constructor with key and value
    final var keyValueEntry = new DictEntry(key1, value1);
    assertSet.accept(keyValueEntry);
    assertEquals(key1, keyValueEntry.key());
    assertEquals(value1, keyValueEntry.value());

    // Constructor with null key should create unset entry
    final var nullKeyEntry = new DictEntry(null, value1);
    assertUnset.accept(nullKeyEntry);
    assertFalse.accept(nullKeyEntry.key()._isSet());
    assertFalse.accept(nullKeyEntry.value()._isSet());


    // Constructor with null value should create unset entry
    final var nullValueEntry = new DictEntry(key1, null);
    assertUnset.accept(nullValueEntry);
    assertFalse.accept(nullValueEntry.key()._isSet());
    assertFalse.accept(nullValueEntry.value()._isSet());


    // Constructor with both null should create unset entry
    final var bothNullEntry = new DictEntry(null, null);
    assertUnset.accept(bothNullEntry);
    assertFalse.accept(bothNullEntry.key()._isSet());
    assertFalse.accept(bothNullEntry.value()._isSet());
  }

  @Test
  void testStaticFactoryMethods() {
    // Factory with no parameters
    final var factoryEmpty = DictEntry._of();
    assertUnset.accept(factoryEmpty);

    // Factory with key and value
    final var factoryWithValues = DictEntry._of(key1, value1);
    assertSet.accept(factoryWithValues);
    assertEquals(key1, factoryWithValues.key());
    assertEquals(value1, factoryWithValues.value());

    // Factory with null key
    final var factoryNullKey = DictEntry._of(null, value1);
    assertUnset.accept(factoryNullKey);

    // Factory with null value
    final var factoryNullValue = DictEntry._of(key1, null);
    assertUnset.accept(factoryNullValue);
  }

  @Test
  void testIsSet() {
    final var unsetEntry = new DictEntry();
    assertNotNull(unsetEntry);
    assertFalse.accept(unsetEntry._isSet());

    final var setEntry = new DictEntry(key1, value1);
    assertNotNull(setEntry);
    assertTrue.accept(setEntry._isSet());

    final var factoryEntry = DictEntry._of(key2, value2);
    assertNotNull(factoryEntry);
    assertTrue.accept(factoryEntry._isSet());
  }

  @Test
  void testKeyAndValueMethods() {
    // Set entry should return key and value
    final var setEntry = new DictEntry(key1, value1);
    assertEquals(key1, setEntry.key());
    assertEquals(value1, setEntry.value());

    final var unsetEntry = new DictEntry();
    assertFalse.accept(unsetEntry.key()._isSet());
    assertFalse.accept(unsetEntry.value()._isSet());

    // Test with different types
    final var mixedEntry = new DictEntry(key3, value3);
    assertEquals(key3, mixedEntry.key());
    assertEquals(value3, mixedEntry.value());
  }

  @Test
  void testEquality() {
    final var entry1 = new DictEntry(key1, value1);
    final var entry2 = new DictEntry(key1, value1);
    final var entry3 = new DictEntry(key2, value2);
    final var entry4 = new DictEntry(key1, value2); // Same key, different value

    // Equality with same key and value
    assertTrue.accept(entry1._eq(entry2));
    assertTrue.accept(entry2._eq(entry1));

    // Inequality with different key and value
    assertFalse.accept(entry1._eq(entry3));
    assertFalse.accept(entry3._eq(entry1));

    // Inequality with same key but different value
    assertFalse.accept(entry1._eq(entry4));
    assertFalse.accept(entry4._eq(entry1));

    // Equality with self
    assertTrue.accept(entry1._eq(entry1));

    // Unset entries comparison
    final var unset1 = new DictEntry();
    final var unset2 = new DictEntry();
    assertUnset.accept(unset1._eq(entry1));
    assertUnset.accept(entry1._eq(unset1));
    assertUnset.accept(unset1._eq(unset2));
  }

  @Test
  void testInequality() {
    final var entry1 = new DictEntry(key1, value1);
    final var entry2 = new DictEntry(key1, value1);
    final var entry3 = new DictEntry(key2, value2);

    // Inequality with same key and value
    assertFalse.accept(entry1._neq(entry2));
    assertFalse.accept(entry2._neq(entry1));

    // Inequality with different key and value
    assertTrue.accept(entry1._neq(entry3));
    assertTrue.accept(entry3._neq(entry1));

    // Unset entries comparison
    final var unsetEntry = new DictEntry();
    assertUnset.accept(unsetEntry._neq(entry1));
    assertUnset.accept(entry1._neq(unsetEntry));
  }

  @Test
  void testStringConversion() {
    // Set entry string conversion
    final var setEntry = new DictEntry(key1, value1);
    final var str = setEntry._string();
    assertSet.accept(str);
    assertNotNull(str.toString());
    assertTrue.accept(Boolean._of(str.toString().contains(key1.toString())));
    assertTrue.accept(Boolean._of(str.toString().contains(value1.toString())));

    // Unset entry string conversion
    final var unsetEntry = new DictEntry();
    final var unsetStr = unsetEntry._string();
    assertUnset.accept(unsetStr);

    // Test string format
    final var stringRep = setEntry.toString();
    assertEquals(key1.toString() + "=" + value1.toString(), stringRep);

    // Unset entry toString
    final var unsetStringRep = unsetEntry.toString();
    assertEquals("", unsetStringRep);
  }

  @Test
  void testHashCode() {
    // Set entry hash code
    final var setEntry = new DictEntry(key1, value1);
    final var hash = setEntry._hashcode();
    assertSet.accept(hash);

    // Unset entry hash code
    final var unsetEntry = new DictEntry();
    final var unsetHash = unsetEntry._hashcode();
    assertUnset.accept(unsetHash);

    // Equal entries should have equal hash codes
    final var entry1 = new DictEntry(key1, value1);
    final var entry2 = new DictEntry(key1, value1);
    assertEquals(entry1._hashcode().state, entry2._hashcode().state);

    // Java hashCode consistency
    assertEquals(entry1.hashCode(), entry2.hashCode());
  }

  @Test
  void testJavaEqualsAndHashCode() {
    final var entry1 = new DictEntry(key1, value1);
    final var entry2 = new DictEntry(key1, value1);
    final var entry3 = new DictEntry(key2, value2);

    // Test equals contract
    assertEquals(entry1, entry1); // reflexive
    assertEquals(entry1, entry2); // symmetric
    assertEquals(entry2, entry1); // symmetric
    assertNotEquals(null, entry1);
    assertNotEquals("not an entry", entry1);

    // Different entries should not be equal
    assertNotEquals(entry1, entry3);

    // Unset entries should be equal
    final var unset2 = new DictEntry();
    assertUnset.accept(unset2);

    // Hash code consistency
    if (entry1.equals(entry2)) {
      assertEquals(entry1.hashCode(), entry2.hashCode());
    }

    // Different entries should have different hash codes (usually)
    assertNotEquals(entry1.hashCode(), entry3.hashCode());
  }

  @Test
  void testEdgeCases() {
    // Test with same key but different value types
    final var stringValueEntry = new DictEntry(key1, value1);
    final var intValueEntry = new DictEntry(key1, value3);
    assertUnset.accept(stringValueEntry._eq(intValueEntry));

    // Test with complex objects as keys and values
    final var complexKey = new List();
    complexKey._addAss(key1);
    complexKey._addAss(key2);
    final var complexValue = new List();
    complexValue._addAss(value1);
    complexValue._addAss(value2);

    final var complexEntry = new DictEntry(complexKey, complexValue);
    assertSet.accept(complexEntry);
    assertEquals(complexKey, complexEntry.key());
    assertEquals(complexValue, complexEntry.value());

    // Test that key and value are stored by reference
    final var originalEntry = new DictEntry(key1, value1);
    final var retrievedKey = originalEntry.key();
    final var retrievedValue = originalEntry.value();

    // Should be the same objects (reference equality for EK9 types)
    assertEquals(key1, retrievedKey);
    assertEquals(value1, retrievedValue);
  }

  @Test
  void testStateTransitions() {
    // Test that once set, entry remains set
    final var entry = new DictEntry(key1, value1);
    assertTrue.accept(entry._isSet());

    // Key and value should remain accessible
    assertEquals(key1, entry.key());
    assertEquals(value1, entry.value());

    // Multiple calls should be consistent
    assertTrue.accept(entry._isSet());
    assertTrue.accept(entry._isSet());
    assertEquals(key1, entry.key());
    assertEquals(value1, entry.value());
  }

  @Test
  void testNullHandling() {
    // Ensure null parameters are handled gracefully
    final var entry = new DictEntry(key1, value1);

    // Equality with null should return unset
    assertUnset.accept(entry._eq(null));
    assertUnset.accept(entry._neq(null));

    // Factory methods with nulls
    final var nullEntry1 = DictEntry._of(null, value1);
    final var nullEntry2 = DictEntry._of(key1, null);
    final var nullEntry3 = DictEntry._of(null, null);

    assertUnset.accept(nullEntry1);
    assertUnset.accept(nullEntry2);
    assertUnset.accept(nullEntry3);
  }
}
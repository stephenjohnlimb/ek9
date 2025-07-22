package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Test DictEntry of (String, String) parameterized type.
 * Tests dual-parameter delegation pattern and type safety for key-value pairs.
 */
@SuppressWarnings({"checkstyle:MethodName", "checkstyle:AbbreviationAsWordInName", "checkstyle:TypeName"})
class _DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487Test extends Common {

  // Test data
  private final String testKey = String._of("testKey");
  private final String testValue = String._of("testValue");
  private final String testKey2 = String._of("anotherKey");
  private final String testValue2 = String._of("anotherValue");

  @Test
  void testConstruction() {
    // Test default constructor
    final var defaultEntry = new _DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487();
    assertNotNull(defaultEntry);
    assertUnset.accept(defaultEntry);
    assertFalse.accept(defaultEntry._isSet());

    // Test two-parameter constructor
    final var valueEntry = new _DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487(testKey, testValue);
    assertNotNull(valueEntry);
    assertSet.accept(valueEntry);
    assertTrue.accept(valueEntry._isSet());
    assertEquals(testKey, valueEntry.key());
    assertEquals(testValue, valueEntry.value());
  }

  @Test
  void testFactoryMethods() {
    // Test _of() - empty entry
    final var unsetEntry = _DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487._of();
    assertNotNull(unsetEntry);
    assertUnset.accept(unsetEntry);

    // Test _of(key, value)
    final var setEntry = _DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487._of(testKey, testValue);
    assertNotNull(setEntry);
    assertSet.accept(setEntry);
    assertEquals(testKey, setEntry.key());
    assertEquals(testValue, setEntry.value());

    // Test _of(DictEntry) with valid entry
    final var baseEntry = DictEntry._of(testKey, testValue);
    final var wrappedEntry = _DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487._of(baseEntry);
    assertNotNull(wrappedEntry);
    assertSet.accept(wrappedEntry);
    assertEquals(testKey, wrappedEntry.key());
    assertEquals(testValue, wrappedEntry.value());

    // Test _of(DictEntry) with null
    final var nullEntry = _DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487._of(null);
    assertNotNull(nullEntry);
    assertUnset.accept(nullEntry);
  }

  @Test
  void testKeyValueAccess() {
    final var entry = _DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487._of(testKey, testValue);
    
    // Test key() method returns correct String type
    final var key = entry.key();
    assertEquals(testKey, key);
    assertEquals(testKey._string().state, key._string().state);

    // Test value() method returns correct String type  
    final var value = entry.value();
    assertEquals(testValue, value);
    assertEquals(testValue._string().state, value._string().state);
  }

  @Test
  void testEqualityOperators() {
    final var entry1 = _DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487._of(testKey, testValue);
    assertNotNull(entry1);
    final var entry2 = _DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487._of(testKey, testValue);
    final var entry3 = _DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487._of(testKey2, testValue2);
    final var unsetEntry = _DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487._of();

    // Test == with same parameterized type
    assertTrue.accept(entry1._eq(entry2));
    assertFalse.accept(entry1._eq(entry3));
    assertUnset.accept(entry1._eq(unsetEntry));
    assertUnset.accept(entry1._eq(null));

    // Test <> with same parameterized type
    assertFalse.accept(entry1._neq(entry2));
    assertTrue.accept(entry1._neq(entry3));
    assertUnset.accept(entry1._neq(unsetEntry));
    assertUnset.accept(entry1._neq(null));

    // Test == with Any (polymorphic)
    assertTrue.accept(entry1._eq((Any) entry2));
    assertFalse.accept(entry1._eq((Any) entry3));
  }

  @Test
  void testComparisonOperators() {
    final var entry1 = _DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487._of(testKey, testValue);
    final var entry2 = _DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487._of(testKey, testValue);
    final var entry3 = _DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487._of(testKey2, testValue2);
    final var unsetEntry = _DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487._of();

    // Test <=> with same parameterized type
    final var cmpResult1 = entry1._cmp(entry2);
    assertSet.accept(cmpResult1);
    assertEquals(0, cmpResult1.state);

    final var cmpResult2 = entry1._cmp(entry3);
    assertSet.accept(cmpResult2);

    assertUnset.accept(entry1._cmp(unsetEntry));
    assertUnset.accept(entry1._cmp(null));

    // Test <=> with Any (polymorphic)
    final var cmpResult3 = entry1._cmp((Any) entry2);
    assertSet.accept(cmpResult3);
    assertEquals(0, cmpResult3.state);
  }

  @Test
  void testStringAndHashcode() {
    final var entry = _DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487._of(testKey, testValue);
    final var unsetEntry = _DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487._of();

    // Test $ (string) operator
    final var stringRep = entry._string();
    assertSet.accept(stringRep);
    assertNotNull(stringRep.state);

    final var unsetStringRep = unsetEntry._string();
    assertUnset.accept(unsetStringRep);

    // Test #? (hashcode) operator
    final var hashcode = entry._hashcode();
    assertSet.accept(hashcode);

    final var unsetHashcode = unsetEntry._hashcode();
    assertUnset.accept(unsetHashcode);

    // Verify consistency
    final var entry2 = _DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487._of(testKey, testValue);
    assertEquals(entry._hashcode(), entry2._hashcode());
  }

  @Test
  void testDelegateAccess() {
    final var entry = _DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487._of(testKey, testValue);
    
    // Test getDelegate() method
    final var delegate = entry.getDelegate();
    assertNotNull(delegate);
    assertSet.accept(delegate);
    assertEquals(testKey, delegate.key());
    assertEquals(testValue, delegate.value());
  }

  @Test
  void testEdgeCasesAndNullHandling() {
    // Test with null key and value
    final var nullKeyEntry = new _DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487(null, testValue);
    assertNotNull(nullKeyEntry);
    // Entry may be set or unset depending on DictEntry base implementation
    
    final var nullValueEntry = new _DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487(testKey, null);
    assertNotNull(nullValueEntry);
    
    final var bothNullEntry = new _DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487(null, null);
    assertNotNull(bothNullEntry);

    // Test factory method with unset String arguments
    final var unsetKey = new String();
    final var unsetValue = new String();
    final var unsetArgsEntry = _DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487._of(unsetKey, unsetValue);
    assertNotNull(unsetArgsEntry);
  }
}
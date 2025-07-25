package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Test Dict of (String, String) parameterized type.
 * Tests two-parameter generic with comprehensive dictionary operations and type safety.
 */
@SuppressWarnings({"checkstyle:MethodName", "checkstyle:AbbreviationAsWordInName", "checkstyle:TypeName"})
class _Dict_E9A1EFF0D62E8EB35F7B0572E7F2C5492D6C980FE8B69376B38612DE6EBEC25FTest extends Common {

  // Factory methods for cleaner object creation
  private static _Dict_E9A1EFF0D62E8EB35F7B0572E7F2C5492D6C980FE8B69376B38612DE6EBEC25F dictStringString() {
    return _Dict_E9A1EFF0D62E8EB35F7B0572E7F2C5492D6C980FE8B69376B38612DE6EBEC25F._of();
  }

  private static _Dict_E9A1EFF0D62E8EB35F7B0572E7F2C5492D6C980FE8B69376B38612DE6EBEC25F dictStringString(String key,
                                                                                                         String value) {
    return _Dict_E9A1EFF0D62E8EB35F7B0572E7F2C5492D6C980FE8B69376B38612DE6EBEC25F._of(key, value);
  }

  private static _Dict_E9A1EFF0D62E8EB35F7B0572E7F2C5492D6C980FE8B69376B38612DE6EBEC25F dictStringStringFromBase(
      Dict base) {
    return _Dict_E9A1EFF0D62E8EB35F7B0572E7F2C5492D6C980FE8B69376B38612DE6EBEC25F._of(base);
  }

  private static _DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487 dictEntryStrStr(String key,
                                                                                                             String value) {
    return _DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487._of(key, value);
  }

  // Test data
  private final String key1 = String._of("option1");
  private final String value1 = String._of("value1");
  private final String key2 = String._of("option2");
  private final String value2 = String._of("value2");
  private final String key3 = String._of("option3");
  private final String value3 = String._of("");

  @Test
  void testConstruction() {
    // Test default constructor
    final var defaultDict = dictStringString();
    assertNotNull(defaultDict);
    assertSet.accept(defaultDict);
    assertTrue.accept(defaultDict._empty());
    assertEquals(0, defaultDict._len().state);

    // Test two-parameter constructor
    final var valueDict = dictStringString(key1, value1);
    assertNotNull(valueDict);
    assertSet.accept(valueDict);
    assertFalse.accept(valueDict._empty());
    assertEquals(1, valueDict._len().state);
    assertTrue.accept(valueDict._contains(key1));
    assertEquals(value1, valueDict.get(key1));
  }

  @Test
  void testFactoryMethods() {
    // Test _of() - empty dict
    final var emptyDict = dictStringString();
    assertNotNull(emptyDict);
    assertSet.accept(emptyDict);
    assertTrue.accept(emptyDict._empty());

    // Test _of(key, value)
    final var setDict = dictStringString(key1, value1);
    assertNotNull(setDict);
    assertSet.accept(setDict);
    assertEquals(1, setDict._len().state);
    assertEquals(value1, setDict.get(key1));

    // Test _of(Dict) with valid dict
    final var baseDict = Dict._of(key1, value1);
    final var wrappedDict = dictStringStringFromBase(baseDict);
    assertNotNull(wrappedDict);
    assertSet.accept(wrappedDict);
    assertEquals(1, wrappedDict._len().state);
    assertEquals(value1, wrappedDict.get(key1));

    // Test _of(Dict) with null
    final var nullDict = dictStringStringFromBase(null);
    assertNotNull(nullDict);
    assertSet.accept(nullDict);
    assertTrue.accept(nullDict._empty());
  }

  @Test
  void testGetMethods() {
    final var dict = dictStringString(key1, value1);

    // Test get() with existing key
    final var result1 = dict.get(key1);
    assertNotNull(result1);
    assertEquals(value1, result1);
    assertEquals(value1._string().state, result1._string().state);

    // Test get() with non-existing key (may throw or return unset based on base implementation)
    try {
      final var result2 = dict.get(key2);
      // If no exception, should return unset String
      assertUnset.accept(result2);
    } catch (Exception _) {
      // Expected behavior based on base Dict implementation
    }

    // Test getOrDefault() 
    final var result3 = dict.getOrDefault(key1, value2);
    assertEquals(value1, result3);

    final var result4 = dict.getOrDefault(key2, value2);
    assertEquals(value2, result4);
  }

  @Test
  void testIteratorMethods() {
    final var dict = dictStringString();
    dict._addAss(dictEntryStrStr(key1, value1));
    dict._addAss(dictEntryStrStr(key2, value2));

    // Test iterator() - returns Iterator of DictEntry of (String, String)
    final var iterator = dict.iterator();
    assertNotNull(iterator);
    assertNotNull(iterator);
    assertSet.accept(iterator);
    assertTrue.accept(iterator.hasNext());

    // Test keys() - returns Iterator of String
    final var keysIterator = dict.keys();
    assertNotNull(keysIterator);
    assertNotNull(keysIterator);
    assertSet.accept(keysIterator);
    assertTrue.accept(keysIterator.hasNext());

    // Test values() - returns Iterator of String
    final var valuesIterator = dict.values();
    assertNotNull(valuesIterator);
    assertNotNull(valuesIterator);
    assertSet.accept(valuesIterator);
    assertTrue.accept(valuesIterator.hasNext());
  }

  @Test
  void testBasicOperators() {
    final var dict1 = dictStringString(key1, value1);
    final var dict2 = dictStringString(key1, value1);
    final var dict3 = dictStringString(key2, value2);
    final var emptyDict = dictStringString();

    // Test ? (isSet)
    assertTrue.accept(dict1._isSet());
    assertTrue.accept(emptyDict._isSet()); // Empty dict is still set

    // Test == with same parameterized type
    assertTrue.accept(dict1._eq(dict2));
    assertFalse.accept(dict1._eq(dict3));
    assertUnset.accept(dict1._eq(null));

    // Test <> with same parameterized type
    assertFalse.accept(dict1._neq(dict2));
    assertTrue.accept(dict1._neq(dict3));
    assertUnset.accept(dict1._neq(null));

    // Test == with Any (polymorphic)
    assertTrue.accept(dict1._eq((Any) dict2));
    assertFalse.accept(dict1._eq((Any) dict3));

    // Test empty and length
    assertFalse.accept(dict1._empty());
    assertTrue.accept(emptyDict._empty());
    assertEquals(1, dict1._len().state);
    assertEquals(0, emptyDict._len().state);

    // Test contains
    assertTrue.accept(dict1._contains(key1));
    assertFalse.accept(dict1._contains(key2));
  }

  @Test
  void testAdditionOperators() {
    final var dict1 = dictStringString(key1, value1);
    final var dict2 = dictStringString(key2, value2);
    final var entry3 = dictEntryStrStr(key3, value3);

    // Test + with same parameterized type
    final var combined = dict1._add(dict2);
    assertNotNull(combined);
    assertSet.accept(combined);
    assertEquals(2, combined._len().state);
    assertTrue.accept(combined._contains(key1));
    assertTrue.accept(combined._contains(key2));

    // Test + with DictEntry of (String, String)
    final var withEntry = dict1._add(entry3);
    assertNotNull(withEntry);
    assertSet.accept(withEntry);
    assertEquals(2, withEntry._len().state);
    assertTrue.accept(withEntry._contains(key1));
    assertTrue.accept(withEntry._contains(key3));

    // Test + with null
    final var withNull = dict1._add((dictStringString().getClass().cast(null)));
    assertNotNull(withNull);
    assertSet.accept(withNull);
  }

  @Test
  void testSubtractionOperators() {
    final var dict1 = dictStringString();
    dict1._addAss(dictEntryStrStr(key1, value1));
    dict1._addAss(dictEntryStrStr(key2, value2));

    final var dict2 = dictStringString(key1, value1);

    // Test - with same parameterized type
    final var subtracted = dict1._sub(dict2);
    assertNotNull(subtracted);
    assertSet.accept(subtracted);
    assertEquals(1, subtracted._len().state);
    assertFalse.accept(subtracted._contains(key1));
    assertTrue.accept(subtracted._contains(key2));

    // Test -= with String key
    final var mutableDict = dictStringString();
    mutableDict._addAss(dictEntryStrStr(key1, value1));
    mutableDict._addAss(dictEntryStrStr(key2, value2));

    assertEquals(2, mutableDict._len().state);
    mutableDict._subAss(key1);
    assertEquals(1, mutableDict._len().state);
    assertFalse.accept(mutableDict._contains(key1));
    assertTrue.accept(mutableDict._contains(key2));
  }

  @Test
  void testAssignmentOperators() {
    final var dict1 = dictStringString();
    final var dict2 = dictStringString();
    final var entry1 = dictEntryStrStr(key1, value1);
    final var entry2 = dictEntryStrStr(key2, value2);

    dict2._addAss(entry2);

    // Test += with same parameterized type
    dict1._addAss(entry1);
    assertEquals(1, dict1._len().state);

    dict1._addAss(dict2);
    assertEquals(2, dict1._len().state);
    assertTrue.accept(dict1._contains(key1));
    assertTrue.accept(dict1._contains(key2));

    // Test += with DictEntry of (String, String)
    final var entry3 = dictEntryStrStr(key3, value3);
    dict1._addAss(entry3);
    assertEquals(3, dict1._len().state);
    assertTrue.accept(dict1._contains(key3));

    // Test | (pipe) with DictEntry
    final var pipeDict = dictStringString();
    pipeDict._pipe(entry1);
    assertEquals(1, pipeDict._len().state);
    assertTrue.accept(pipeDict._contains(key1));
  }

  @Test
  void testMutatingOperators() {
    final var dict1 = dictStringString(key1, value1);
    final var dict2 = dictStringString(key2, value2);
    final var dict3 = dictStringString(key1, value3);

    // Test :~: (merge)
    dict1._merge(dict2);
    assertEquals(2, dict1._len().state);
    assertTrue.accept(dict1._contains(key1));
    assertTrue.accept(dict1._contains(key2));
    assertEquals(value1, dict1.get(key1));
    assertEquals(value2, dict1.get(key2));

    // Test :^: (replace)  
    final var replaceDict = dictStringString(key1, value1);
    replaceDict._replace(dict3);
    assertEquals(value3, replaceDict.get(key1));

    // Test :=: (copy)
    final var copyDict = dictStringString();
    copyDict._copy(dict2);
    assertEquals(1, copyDict._len().state);
    assertTrue.accept(copyDict._contains(key2));
    assertEquals(value2, copyDict.get(key2));

    // Test with null arguments
    dict1._merge(null); // Should handle gracefully
    dict1._replace(null); // Should handle gracefully
    dict1._copy(null); // Should handle gracefully
  }

  @Test
  void testStringAndHashcode() {
    final var dict = dictStringString(key1, value1);
    final var emptyDict = dictStringString();

    // Test $ (string) operator
    final var stringRep = dict._string();
    assertSet.accept(stringRep);
    assertNotNull(stringRep.state);

    final var emptyStringRep = emptyDict._string();
    assertSet.accept(emptyStringRep);

    // Test #? (hashcode) operator
    final var hashcode = dict._hashcode();
    assertSet.accept(hashcode);

    final var emptyHashcode = emptyDict._hashcode();
    assertSet.accept(emptyHashcode);

    // Verify consistency
    final var dict2 = dictStringString(key1, value1);
    assertEquals(dict._hashcode(), dict2._hashcode());
  }

  @Test
  void testEdgeCasesAndNullHandling() {
    // Test with null keys and values
    final var nullKeyDict = dictStringString(null, value1);
    assertNotNull(nullKeyDict);

    final var nullValueDict = dictStringString(key1, null);
    assertNotNull(nullValueDict);

    // Test with unset String arguments
    final var unsetKey = new String();
    final var unsetValue = new String();
    final var unsetArgsDict = dictStringString(unsetKey, unsetValue);
    assertNotNull(unsetArgsDict);

    // Test operations with empty string values (common in GetOpt usage)
    final var emptyValueDict = dictStringString(key1, String._of(""));
    assertSet.accept(emptyValueDict);
    assertTrue.accept(emptyValueDict._contains(key1));
    assertEquals("", emptyValueDict.get(key1)._string().state);
  }

  @Test
  void testGetOptUsageScenario() {
    // Test typical GetOpt pattern usage
    final var options = dictStringString();

    // Add command-line options
    options._addAss(dictEntryStrStr(String._of("-v"), String._of(""))); // Flag option
    options._addAss(dictEntryStrStr(String._of("-f"), String._of("filename.txt"))); // Value option
    options._addAss(dictEntryStrStr(String._of("-d"), String._of("2"))); // Numeric value option

    assertEquals(3, options._len().state);

    // Test checking for flags
    assertTrue.accept(options._contains(String._of("-v")));
    assertTrue.accept(options._contains(String._of("-f")));
    assertTrue.accept(options._contains(String._of("-d")));
    assertFalse.accept(options._contains(String._of("-x")));

    // Test getting values
    assertEquals("", options.get(String._of("-v"))._string().state);
    assertEquals("filename.txt", options.get(String._of("-f"))._string().state);
    assertEquals("2", options.get(String._of("-d"))._string().state);
  }
}
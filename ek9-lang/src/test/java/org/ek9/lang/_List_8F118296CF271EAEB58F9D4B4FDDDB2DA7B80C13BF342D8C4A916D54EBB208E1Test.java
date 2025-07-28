package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for List of String parameterized type.
 * Tests all 27 methods from base List with String type safety and delegation behavior.
 */
@SuppressWarnings({"checkstyle:MethodName", "checkstyle:AbbreviationAsWordInName", "checkstyle:TypeName"})
class _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1Test extends Common {

  // Type aliases for cleaner code
  private static final Class<_List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1> LIST_STRING = 
      _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1.class;

  private static final Class<_Iterator_852BE8F78E9C7E622E0E2BDC5523BEFF664305AD702B04CE0463ED42C1FE2CA2> ITERATOR_STRING = 
      _Iterator_852BE8F78E9C7E622E0E2BDC5523BEFF664305AD702B04CE0463ED42C1FE2CA2.class;

  // Factory methods for cleaner object creation
  private static _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1 listString() {
    return _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1._of();
  }

  private static _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1 listString(String value) {
    return new _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1(value);
  }

  private static _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1 listStringFromBase(List base) {
    return _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1._of(base);
  }

  private static _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1 listStringFromJava(java.util.List<Any> javaList) {
    return _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1._of(javaList);
  }

  // Test data setup
  private final String testString1 = String._of("Hello");
  private final String testString2 = String._of("World");
  private final String testString3 = String._of("Test");

  @Test
  void testConstruction() {
    // Test default constructor
    final var defaultList = listString();
    assertNotNull(defaultList);
    assertSet.accept(defaultList);
    assertTrue.accept(defaultList._isSet());
    assertTrue.accept(defaultList._empty());
    assertEquals(Integer._of(0), defaultList._len());

    // Test value constructor
    final var valueList = listString(testString1);
    assertNotNull(valueList);
    assertSet.accept(valueList);
    assertFalse.accept(valueList._empty());
    assertEquals(Integer._of(1), valueList._len());
    assertEquals(testString1, valueList.first());
  }

  @Test
  void testFactoryMethods() {
    // Test _of() - empty list
    final var emptyList = listString();
    assertNotNull(emptyList);
    assertSet.accept(emptyList);
    assertTrue.accept(emptyList._empty());

    // Test _of(List) - from base List
    final var baseList = new List(testString1);
    baseList._addAss(testString2);
    final var fromBase = listStringFromBase(baseList);
    assertNotNull(fromBase);
    assertSet.accept(fromBase);
    assertEquals(Integer._of(2), fromBase._len());
    assertEquals(testString1, fromBase.first());
    assertEquals(testString2, fromBase.last());

    // Test _of(null List)
    final var fromNull = listStringFromBase(null);
    assertNotNull(fromNull);
    assertSet.accept(fromNull);
    assertTrue.accept(fromNull._empty());

    // Test _of(java.util.List<Any>)
    final var javaList = new java.util.ArrayList<Any>();
    javaList.add(testString1);
    javaList.add(testString2);
    final var fromJavaList = listStringFromJava(javaList);
    assertNotNull(fromJavaList);
    assertSet.accept(fromJavaList);
    assertEquals(Integer._of(2), fromJavaList._len());
  }

  @Test
  void testAccessMethods() {
    // Setup test list
    final var list = listString();
    list._addAss(testString1);
    list._addAss(testString2);
    list._addAss(testString3);

    // Test get() with valid indices
    assertEquals(testString1, list.get(Integer._of(0)));
    assertEquals(testString2, list.get(Integer._of(1)));
    assertEquals(testString3, list.get(Integer._of(2)));

    // Test get() with invalid index
    assertThrows(Exception.class, () -> list.get(Integer._of(10)));
    assertThrows(Exception.class, () -> list.get(Integer._of(-1)));

    // Test first() and last()
    assertEquals(testString1, list.first());
    assertEquals(testString3, list.last());
    assertEquals(String.class, list.first().getClass());
    assertEquals(String.class, list.last().getClass());

    // Test _prefix() and _suffix() operators (same as first/last)
    assertEquals(testString1, list._prefix());
    assertEquals(testString3, list._suffix());
  }

  @Test
  void testAccessMethodsOnEmptyList() {
    final var emptyList = listString();

    // Test exceptions on empty list
    assertThrows(Exception.class, emptyList::first);
    assertThrows(Exception.class, emptyList::last);
    assertThrows(Exception.class, emptyList::_prefix);
    assertThrows(Exception.class, emptyList::_suffix);
  }

  @Test
  void testIteratorIntegration() {
    // Test iterator() returns correct parameterized type
    final var list = listString();
    list._addAss(testString1);
    list._addAss(testString2);

    final var iterator = list.iterator();
    assertNotNull(iterator);
    assertEquals(ITERATOR_STRING, iterator.getClass());
    assertSet.accept(iterator);

    // Test iteration produces String objects
    assertTrue.accept(iterator.hasNext());
    final var first = iterator.next();
    assertEquals(testString1, first);
    assertEquals(String.class, first.getClass());

    assertTrue.accept(iterator.hasNext());
    final var second = iterator.next();
    assertEquals(testString2, second);
    assertEquals(String.class, second.getClass());

    assertFalse.accept(iterator.hasNext());
  }

  @Test
  void testIteratorWorkflow() {
    // Test complete iteration workflow
    final var list = listString();
    final var expected = new String[]{testString1, testString2, testString3};
    
    for (String s : expected) {
      list._addAss(s);
    }

    int count = 0;
    final var iter = list.iterator();
    while (iter.hasNext().state) {
      final var value = iter.next();
      assertEquals(expected[count], value);
      assertEquals(String.class, value.getClass());
      count++;
    }
    assertEquals(3, count);
  }

  @Test
  void testListTransformations() {
    // Setup test list
    final var list = listString();
    list._addAss(testString1);
    list._addAss(testString2);
    list._addAss(testString3);

    // Test reverse() returns new List of String
    final var reversed = list.reverse();
    assertNotNull(reversed);
    assertEquals(LIST_STRING, reversed.getClass());
    assertEquals(Integer._of(3), reversed._len());
    assertEquals(testString3, reversed.first()); // Reversed order
    assertEquals(testString1, reversed.last());

    // Original list should be unchanged
    assertEquals(testString1, list.first());
    assertEquals(testString3, list.last());

    // Test _negate() operator (same as reverse)
    final var negated = list._negate();
    assertEquals(testString3, negated.first());
    assertEquals(testString1, negated.last());
  }

  @Test
  void testArithmeticOperators() {
    // Setup test lists
    final var list1 = listString();
    list1._addAss(testString1);
    list1._addAss(testString2);

    final var list2 = listString();
    list2._addAss(testString3);

    // Test + operator with List of String
    final var added = list1._add(list2);
    assertNotNull(added);
    assertEquals(LIST_STRING, added.getClass());
    assertEquals(Integer._of(3), added._len());
    assertEquals(testString1, added.get(Integer._of(0)));
    assertEquals(testString2, added.get(Integer._of(1)));
    assertEquals(testString3, added.get(Integer._of(2)));

    // Test + operator with String
    final var addedString = list1._add(testString3);
    assertEquals(Integer._of(3), addedString._len());
    assertEquals(testString3, addedString.last());

    // Test - operator with List of String
    final var listWithDuplicates = listString();
    listWithDuplicates._addAss(testString1);
    listWithDuplicates._addAss(testString2);
    listWithDuplicates._addAss(testString3); // Different string, not duplicate

    final var removed = listWithDuplicates._sub(list1);  // Remove list1 (testString1, testString2)
    assertEquals(Integer._of(1), removed._len()); // Only testString3 remains

    // Test - operator with String
    final var removedString = listWithDuplicates._sub(testString1);
    assertEquals(Integer._of(2), removedString._len()); // One instance removed
  }

  @Test
  void testEqualityOperators() {
    // Setup identical lists
    final var list1 = listString();
    assertNotNull(list1);
    list1._addAss(testString1);
    list1._addAss(testString2);

    final var list2 = listString();
    list2._addAss(testString1);
    list2._addAss(testString2);

    final var list3 = listString();
    list3._addAss(testString3);

    // Test _eq with List of String
    assertTrue.accept(list1._eq(list2));
    assertFalse.accept(list1._eq(list3));
    assertUnset.accept(list1._eq(null));

    // Test _eq with Any (polymorphic)
    assertTrue.accept(list1._eq((Any) list2));
    assertFalse.accept(list1._eq((Any) list3));

    // Test _neq operator
    assertFalse.accept(list1._neq(list2));
    assertTrue.accept(list1._neq(list3));
    assertUnset.accept(list1._neq(null));
  }

  @Test
  void testQueryOperators() {
    // Setup test list
    final var list = listString();
    list._addAss(testString1);
    list._addAss(testString2);

    // Test _isSet operator
    assertTrue.accept(list._isSet());

    // Test _empty operator
    assertFalse.accept(list._empty());
    final var emptyList = listString();
    assertTrue.accept(emptyList._empty());

    // Test _len operator
    assertEquals(Integer._of(2), list._len());
    assertEquals(Integer._of(0), emptyList._len());

    // Test _contains operator with String
    assertTrue.accept(list._contains(testString1));
    assertTrue.accept(list._contains(testString2));
    assertFalse.accept(list._contains(testString3));
    assertUnset.accept(list._contains(null));

    // Test _string operator
    assertSet.accept(list._string());
    assertNotNull(list._string().state);

    // Test _hashcode operator
    assertSet.accept(list._hashcode());
  }

  @Test
  void testAssignmentOperators() {
    // Test _copy operator
    final var sourceList = listString();
    sourceList._addAss(testString1);
    sourceList._addAss(testString2);

    final var targetList = listString();
    targetList._copy(sourceList);
    assertEquals(Integer._of(2), targetList._len());
    assertEquals(testString1, targetList.first());
    assertEquals(testString2, targetList.last());

    // Test _replace operator (same as copy)
    final var replaceTarget = listString();
    replaceTarget._addAss(testString3);
    replaceTarget._replace(sourceList);
    assertEquals(Integer._of(2), replaceTarget._len());
    assertEquals(testString1, replaceTarget.first());

    // Test null handling
    final var nullTarget = listString();
    nullTarget._addAss(testString1);
    assertDoesNotThrow(() -> nullTarget._copy(null));
    assertDoesNotThrow(() -> nullTarget._replace(null));
  }

  @Test
  void testMergeOperators() {
    // Setup test list
    final var list1 = listString();
    list1._addAss(testString1);

    final var list2 = listString();
    list2._addAss(testString2);
    list2._addAss(testString3);

    // Test _merge with List of String
    list1._merge(list2);
    assertEquals(Integer._of(3), list1._len());
    assertEquals(testString1, list1.get(Integer._of(0)));
    assertEquals(testString2, list1.get(Integer._of(1)));
    assertEquals(testString3, list1.get(Integer._of(2)));

    // Test _merge with String
    final var mergeTarget = listString();
    mergeTarget._merge(testString1);
    assertEquals(Integer._of(1), mergeTarget._len());
    assertEquals(testString1, mergeTarget.first());

    // Test _pipe operator (same as merge with String)
    final var pipeTarget = listString();
    pipeTarget._pipe(testString1);
    assertEquals(Integer._of(1), pipeTarget._len());
    assertEquals(testString1, pipeTarget.first());
  }

  @Test
  void testAssignmentArithmeticOperators() {
    // Test += with List of String
    final var list1 = listString();
    list1._addAss(testString1);

    final var list2 = listString();
    list2._addAss(testString2);

    list1._addAss(list2);
    assertEquals(Integer._of(2), list1._len());
    assertEquals(testString2, list1.last());

    // Test += with String
    list1._addAss(testString3);
    assertEquals(Integer._of(3), list1._len());
    assertEquals(testString3, list1.last());

    // Test -= with List of String
    final var subList = listString();
    subList._addAss(testString2);
    list1._subAss(subList);
    assertEquals(Integer._of(2), list1._len()); // testString2 removed

    // Test -= with String
    list1._subAss(testString1);
    assertEquals(Integer._of(1), list1._len()); // testString1 removed
  }

  @Test
  void testEdgeCasesAndNullHandling() {
    final var list = listString();

    // Test operations with null parameters  
    assertDoesNotThrow(() -> list._merge((String) null));
    assertDoesNotThrow(() -> list._pipe((String) null));
    assertDoesNotThrow(() -> list._addAss((String) null));
    assertDoesNotThrow(() -> list._subAss((String) null));

    // Test operations with null List parameters (cast to disambiguate)
    assertDoesNotThrow(() -> list._merge((_List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1) null));
    assertDoesNotThrow(() -> list._addAss((_List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1) null));
    assertDoesNotThrow(() -> list._subAss((_List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1) null));

    // Test arithmetic operations with null (cast to disambiguate)
    final var result1 = list._add((String) null);
    assertNotNull(result1);
    
    final var result2 = list._sub((String) null);
    assertNotNull(result2);

    final var result3 = list._add((_List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1) null);
    assertNotNull(result3);

    final var result4 = list._sub((_List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1) null);
    assertNotNull(result4);

    // Test empty string handling
    final var emptyString = String._of("");
    list._addAss(emptyString);
    assertTrue.accept(list._contains(emptyString));
    assertEquals(emptyString, list.first());
  }

  @Test
  void testTypeConsistencyWithBase() {
    // Create base List with Strings
    final var baseList = new List(testString1);
    baseList._addAss(testString2);

    // Create parameterized list from base
    final var paramList = listStringFromBase(baseList);

    // Both should have consistent behavior
    assertEquals(baseList._len(), paramList._len());
    assertEquals(baseList._empty(), paramList._empty());
    assertEquals(baseList._isSet(), paramList._isSet());

    // Parameterized version should provide String-typed access
    assertEquals(testString1, paramList.first());
    assertEquals(String.class, paramList.first().getClass());
    assertEquals(testString2, paramList.last());
    assertEquals(String.class, paramList.last().getClass());
  }

  @Test
  void testCompleteOperatorCoverage() {
    // Verify all operators work without exceptions
    final var list = listString();
    list._addAss(testString1);

    // Test all query operators
    assertNotNull(list._isSet());
    assertNotNull(list._empty());
    assertNotNull(list._len());
    assertNotNull(list._string());
    assertNotNull(list._hashcode());
    assertNotNull(list._contains(testString1));

    // Test all transformation operators
    assertNotNull(list.reverse());
    assertNotNull(list._negate());
    assertNotNull(list.iterator());

    // Test all arithmetic operators
    assertNotNull(list._add(testString2));
    assertNotNull(list._sub(testString1));
    assertNotNull(list._add(list));
    assertNotNull(list._sub(list));

    // Test all access operators
    assertNotNull(list._prefix());
    assertNotNull(list._suffix());
    assertNotNull(list.first());
    assertNotNull(list.last());
    assertNotNull(list.get(Integer._of(0)));
  }

  @Test
  void testAsJson() {
    // Test empty List of String - should be empty JSON array
    final var emptyList = listString();
    final var emptyJson = emptyList._json();
    assertSet.accept(emptyJson);
    assertTrue.accept(emptyJson.arrayNature());
    assertTrue.accept(emptyJson._empty());
    assertEquals(Integer._of(0), emptyJson._len());

    // Test List with single string
    final var singleList = listString(testString1);
    final var singleJson = singleList._json();
    assertSet.accept(singleJson);
    assertTrue.accept(singleJson.arrayNature());
    assertEquals(Integer._of(1), singleJson._len());

    // Verify the first element matches the string's JSON representation
    final var firstElement = singleJson.get(Integer._of(0));
    assertSet.accept(firstElement);
    final var expectedStringJson = testString1._json();
    assertTrue.accept(firstElement._eq(expectedStringJson));

    // Test List with multiple strings
    final var multiList = listString();
    multiList._addAss(testString1);
    multiList._addAss(testString2);
    multiList._addAss(testString3);

    final var multiJson = multiList._json();
    assertSet.accept(multiJson);
    assertTrue.accept(multiJson.arrayNature());
    assertEquals(Integer._of(3), multiJson._len());

    // Verify each element matches its corresponding string's JSON representation
    final var element0 = multiJson.get(Integer._of(0));
    final var element1 = multiJson.get(Integer._of(1));
    final var element2 = multiJson.get(Integer._of(2));

    assertSet.accept(element0);
    assertSet.accept(element1);
    assertSet.accept(element2);

    assertTrue.accept(element0._eq(testString1._json()));
    assertTrue.accept(element1._eq(testString2._json()));
    assertTrue.accept(element2._eq(testString3._json()));

    // Note: Collections are always set even when empty, so no unset test needed
  }

  @Test
  void testJsonStructureAsString() {
    // Test that clearly shows the actual JSON structure as pretty-printed strings
    // This makes it very clear what the JSON array structure looks like

    // Test empty List of String - should produce empty JSON array
    final var emptyList = listString();
    final var emptyJson = emptyList._json();
    
    final var expectedEmptyJson = """
        []"""; // Empty JSON array
    
    assertEquals(expectedEmptyJson.trim(), emptyJson._string().state.trim());

    // Test List with single String element
    final var singleList = listString(testString1);
    final var singleJson = singleList._json();
    
    final var expectedSingleJson = """
        ["Hello"]"""; // Array with one String element
    
    assertEquals(expectedSingleJson.trim(), singleJson._string().state.trim());

    // Test List with multiple String elements showing full structure
    final var multiList = listString();
    multiList._addAss(testString1); // "Hello"
    multiList._addAss(testString2); // "World"
    multiList._addAss(testString3); // "Test"
    
    final var multiJson = multiList._json();
    
    final var expectedMultiJson = """
        ["Hello","World","Test"]"""; // Array with multiple String elements
    
    assertEquals(expectedMultiJson.trim(), multiJson._string().state.trim());

    // Test List with edge case strings
    final var specialList = listString();
    specialList._addAss(String._of(""));           // Empty string
    specialList._addAss(String._of("with spaces")); // String with spaces
    specialList._addAss(String._of("special-chars!@#")); // String with special characters
    
    final var specialJson = specialList._json();
    
    final var expectedSpecialJson = """
        ["","with spaces","special-chars!@#"]"""; // Array with edge case strings
    
    assertEquals(expectedSpecialJson.trim(), specialJson._string().state.trim());

    // Test List with longer content to show structure scales
    final var longList = listString();
    longList._addAss(String._of("First"));
    longList._addAss(String._of("Second"));
    longList._addAss(String._of("Third"));
    longList._addAss(String._of("Fourth"));
    longList._addAss(String._of("Fifth"));
    
    final var longJson = longList._json();
    
    final var expectedLongJson = """
        ["First","Second","Third","Fourth","Fifth"]"""; // Array with many String elements
    
    assertEquals(expectedLongJson.trim(), longJson._string().state.trim());
  }
}
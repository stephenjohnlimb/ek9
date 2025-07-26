package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

@SuppressWarnings({"checkstyle:MethodName", "checkstyle:AbbreviationAsWordInName", "checkstyle:TypeName"})
class _Iterator_8A55E2CE14B3B336B88069A9954BBCB8C58B64CA55768EECCED18381C1DA376CTest extends Common {

  // Factory methods for cleaner object creation
  private static _Iterator_8A55E2CE14B3B336B88069A9954BBCB8C58B64CA55768EECCED18381C1DA376C iterJSON() {
    return new _Iterator_8A55E2CE14B3B336B88069A9954BBCB8C58B64CA55768EECCED18381C1DA376C();
  }
  
  private static _Iterator_8A55E2CE14B3B336B88069A9954BBCB8C58B64CA55768EECCED18381C1DA376C iterJSON(JSON value) {
    return new _Iterator_8A55E2CE14B3B336B88069A9954BBCB8C58B64CA55768EECCED18381C1DA376C(value);
  }
  
  private static _Iterator_8A55E2CE14B3B336B88069A9954BBCB8C58B64CA55768EECCED18381C1DA376C iterJSONFromBase(Iterator base) {
    return _Iterator_8A55E2CE14B3B336B88069A9954BBCB8C58B64CA55768EECCED18381C1DA376C._of(base);
  }
  
  // Helper methods for set/unset assertions
  private void assertIteratorSet(_Iterator_8A55E2CE14B3B336B88069A9954BBCB8C58B64CA55768EECCED18381C1DA376C iterator) {
    assertSet.accept(iterator);
    assertTrue.accept(iterator.hasNext());
  }
  
  private void assertIteratorUnset(_Iterator_8A55E2CE14B3B336B88069A9954BBCB8C58B64CA55768EECCED18381C1DA376C iterator) {
    assertUnset.accept(iterator);
    assertFalse.accept(iterator.hasNext());
  }

  @Test
  void testConstructionAndBasicOperations() {
    // Test default constructor
    final var defaultIterator = iterJSON();
    assertIteratorUnset(defaultIterator);

    // Test constructor with single element
    final var json1 = JSON._of("\"test\"");
    final var singleElementIterator = iterJSON(json1);
    assertIteratorSet(singleElementIterator);
    assertEquals(json1, singleElementIterator.next());
    assertIteratorUnset(singleElementIterator);
  }

  @Test
  void testFactoryMethodsAndDelegation() {
    // Test _of() factory method - empty iterator
    final var unset1 = _Iterator_8A55E2CE14B3B336B88069A9954BBCB8C58B64CA55768EECCED18381C1DA376C._of();
    assertNotNull(unset1);
    assertIteratorUnset(unset1);

    // Test _of(null) factory method
    final var unset2 = iterJSONFromBase(null);
    assertNotNull(unset2);
    assertIteratorUnset(unset2);

    // Test _of(Iterator) factory method - from base Iterator
    final var testJSON = JSON_42;
    final var set1 = iterJSONFromBase(Iterator._of(testJSON));
    assertNotNull(set1);
    assertIteratorSet(set1);
    assertEquals(testJSON, set1.next());
    assertIteratorUnset(set1);
  }

  @Test
  void testOperatorsAndTypeSafety() {
    // Test equality and operators
    final var json1 = JSON._of("\"hello\"");
    final var iter1 = iterJSON(json1);
    final var iter2 = iterJSON(json1);
    assertFalse.accept(iter1._eq(iter2));
    assertNotEquals(iter1, iter2);
    assertNotEquals(iter1.hashCode(), iter2.hashCode());
    
    // Test iteration over multiple JSON values
    final var jsonList = new java.util.ArrayList<Any>();
    jsonList.add(JSON._of("\"first\""));
    jsonList.add(JSON_42);
    jsonList.add(JSON_TRUE);
    
    final var multiElementIterator = iterJSONFromBase(Iterator._of(jsonList));
    assertIteratorSet(multiElementIterator);
    assertEquals(JSON._of("\"first\""), multiElementIterator.next());
    assertEquals(JSON_42, multiElementIterator.next());
    assertEquals(JSON_TRUE, multiElementIterator.next());
    assertIteratorUnset(multiElementIterator);
  }

  @Test
  void testJSONSpecificIteration() {
    // Test with different JSON types
    final var stringJson = JSON._of("\"text\"");
    final var numberJson = JSON._of("123");
    final var booleanJson = JSON._of("false");
    final var nullJson = JSON_NULL;
    
    // Create list with mixed JSON types
    final var mixedJsonList = new java.util.ArrayList<Any>();
    mixedJsonList.add(stringJson);
    mixedJsonList.add(numberJson);
    mixedJsonList.add(booleanJson);
    mixedJsonList.add(nullJson);
    
    final var mixedIterator = iterJSONFromBase(Iterator._of(mixedJsonList));
    assertIteratorSet(mixedIterator);
    
    // Verify each JSON element comes back as JSON type
    final var first = mixedIterator.next();
    assertEquals(stringJson, first);
    assertEquals("\"text\"", first.toString());
    
    final var second = mixedIterator.next();
    assertEquals(numberJson, second);
    assertEquals("123", second.toString());
    
    final var third = mixedIterator.next();
    assertEquals(booleanJson, third);
    assertEquals("false", third.toString());
    
    final var fourth = mixedIterator.next();
    assertEquals(nullJson, fourth);
    assertEquals("null", fourth.toString());
    
    assertIteratorUnset(mixedIterator);
  }

  @Test
  void testHashcodeAndEquality() {
    // Test hashcode functionality
    final var json1 = JSON._of("\"test\"");
    final var iter1 = iterJSON(json1);
    
    final var hashResult = iter1._hashcode();
    assertSet.accept(hashResult);
    
    // Test that different iterators have different hashcodes
    final var json2 = JSON._of("\"different\"");
    final var iter2 = iterJSON(json2);
    assertNotEquals(iter1._hashcode().state, iter2._hashcode().state);
  }

  @Test
  void testEmptyAndUnsetIterators() {
    // Test empty iterator from empty base
    final var emptyList = new java.util.ArrayList<Any>();
    final var emptyIterator = iterJSONFromBase(Iterator._of(emptyList));
    assertNotNull(emptyIterator);
    assertIteratorUnset(emptyIterator);
    
    // Test unset JSON element handling
    final var unsetJson = new JSON();
    final var unsetJsonList = new java.util.ArrayList<Any>();
    unsetJsonList.add(unsetJson);
    
    final var iterWithUnset = iterJSONFromBase(Iterator._of(unsetJsonList));
    assertIteratorSet(iterWithUnset);
    final var retrievedUnset = iterWithUnset.next();
    assertUnset.accept(retrievedUnset);
    assertIteratorUnset(iterWithUnset);
  }
}
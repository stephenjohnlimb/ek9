package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Test Iterator of DictEntry of (String, String) parameterized type.
 * Tests nested parameterization with type-safe iteration over key-value pairs.
 */
@SuppressWarnings({"checkstyle:MethodName", "checkstyle:AbbreviationAsWordInName", "checkstyle:TypeName"})
class _Iterator_66111C2B710ABD8FDA1988CF5DDC4BBC71C07280CA899D51F77E28BB2FE7717DTest extends Common {

  // Type aliases for cleaner code

  private static final Class<_DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487> DICT_ENTRY_SS = 
      _DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487.class;

  // Test data
  private final String testKey = String._of("key1");
  private final String testValue = String._of("value1");
  
  // Factory methods for cleaner object creation
  private static _Iterator_66111C2B710ABD8FDA1988CF5DDC4BBC71C07280CA899D51F77E28BB2FE7717D iterDictEntryStrStr() {
    return new _Iterator_66111C2B710ABD8FDA1988CF5DDC4BBC71C07280CA899D51F77E28BB2FE7717D();
  }
  
  private static _Iterator_66111C2B710ABD8FDA1988CF5DDC4BBC71C07280CA899D51F77E28BB2FE7717D iterDictEntryStrStr(
      _DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487 entry) {
    return new _Iterator_66111C2B710ABD8FDA1988CF5DDC4BBC71C07280CA899D51F77E28BB2FE7717D(entry);
  }
  
  private static _Iterator_66111C2B710ABD8FDA1988CF5DDC4BBC71C07280CA899D51F77E28BB2FE7717D iterDictEntryStrStrFromBase(Iterator base) {
    return _Iterator_66111C2B710ABD8FDA1988CF5DDC4BBC71C07280CA899D51F77E28BB2FE7717D._of(base);
  }
  
  private _DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487 testEntry() {
    return _DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487._of(testKey, testValue);
  }
  
  // Helper methods for set/unset assertions
  private void assertIteratorSet(_Iterator_66111C2B710ABD8FDA1988CF5DDC4BBC71C07280CA899D51F77E28BB2FE7717D iterator) {
    assertSet.accept(iterator);
    assertTrue.accept(iterator.hasNext());
  }
  
  private void assertIteratorUnset(_Iterator_66111C2B710ABD8FDA1988CF5DDC4BBC71C07280CA899D51F77E28BB2FE7717D iterator) {
    assertUnset.accept(iterator);
    assertFalse.accept(iterator.hasNext());
  }

  @Test
  void testConstructionAndBasicOperations() {
    // Test default constructor
    final var defaultIterator = iterDictEntryStrStr();
    assertNotNull(defaultIterator);
    assertIteratorUnset(defaultIterator);

    // Test value constructor with DictEntry of (String, String)
    final var entry = testEntry();
    final var valueIterator = iterDictEntryStrStr(entry);
    assertNotNull(valueIterator);
    assertIteratorSet(valueIterator);
  }

  @Test
  void testFactoryMethodsAndDelegation() {
    // Test _of() factory method - empty iterator
    final var unsetIterator = _Iterator_66111C2B710ABD8FDA1988CF5DDC4BBC71C07280CA899D51F77E28BB2FE7717D._of();
    assertNotNull(unsetIterator);
    assertIteratorUnset(unsetIterator);

    // Test _of(null) factory method
    final var unsetIterator2 = iterDictEntryStrStrFromBase(null);
    assertNotNull(unsetIterator2);
    assertIteratorUnset(unsetIterator2);

    // Test _of(Iterator) factory method - from base Iterator
    final var baseEntry = DictEntry._of(testKey, testValue);
    final var baseIterator = Iterator._of(baseEntry);
    final var wrappedIterator = iterDictEntryStrStrFromBase(baseIterator);
    assertNotNull(wrappedIterator);
    assertIteratorSet(wrappedIterator);
  }

  @Test
  void testTypeSafetyAndIteration() {
    // Create an iterator with a single DictEntry of (String, String)
    final var entry = testEntry();
    final var iterator = iterDictEntryStrStr(entry);

    // Test hasNext() and next() pattern with type safety
    assertIteratorSet(iterator);
    final var nextEntry = iterator.next();
    assertNotNull(nextEntry);
    assertSet.accept(nextEntry);
    assertEquals(testKey, nextEntry.key());
    assertEquals(testValue, nextEntry.value());
    
    // Verify it's the correct parameterized type
    assertInstanceOf(DICT_ENTRY_SS, nextEntry);
    assertInstanceOf(String.class, nextEntry.key());
    assertInstanceOf(String.class, nextEntry.value());

    // After consuming the single element, iterator should be empty
    assertIteratorUnset(iterator);
  }


  @Test
  void testOperatorsAndBehavior() {
    // Test operators with different states
    final var entry1 = testEntry();
    final var iterator1 = iterDictEntryStrStr(entry1);
    final var emptyIterator = iterDictEntryStrStr();
    
    // Test equality
    assertUnset.accept(iterator1._eq(null));

    // Test hashcode operator
    assertSet.accept(iterator1._hashcode());
    assertSet.accept(emptyIterator._hashcode());
    
    // Test _isSet behavior
    assertIteratorSet(iterator1);
    iterator1.next(); // Consume element
    assertIteratorUnset(iterator1);
  }


  @Test
  void testEdgeCasesAndMultipleElements() {
    // Test with unset DictEntry
    final var unsetEntry = new _DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487();
    final var iteratorWithUnsetEntry = iterDictEntryStrStr(unsetEntry);
    assertNotNull(iteratorWithUnsetEntry);

    // Test factory method with unset Iterator
    final var unsetBaseIterator = new Iterator();
    final var iteratorFromUnset = iterDictEntryStrStrFromBase(unsetBaseIterator);
    assertNotNull(iteratorFromUnset);
    assertIteratorUnset(iteratorFromUnset);
    
    // Test multiple element iteration pattern
    final var entry1 = DictEntry._of(String._of("key1"), String._of("value1"));
    final var baseIterator = Iterator._of(entry1);
    final var paramIterator = iterDictEntryStrStrFromBase(baseIterator);

    int count = 0;
    while (paramIterator.hasNext().state && count < 10) { // Safety limit
      final var nextEntry = paramIterator.next();
      assertNotNull(nextEntry);
      assertSet.accept(nextEntry);
      assertInstanceOf(DICT_ENTRY_SS, nextEntry);
      count++;
    }
  }
}
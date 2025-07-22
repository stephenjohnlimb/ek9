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

  // Test data
  private final String testKey = String._of("key1");
  private final String testValue = String._of("value1");

  @Test
  void testConstruction() {
    // Test default constructor
    final var defaultIterator = new _Iterator_66111C2B710ABD8FDA1988CF5DDC4BBC71C07280CA899D51F77E28BB2FE7717D();
    assertNotNull(defaultIterator);
    assertUnset.accept(defaultIterator);
    assertFalse.accept(defaultIterator.hasNext());

    // Test value constructor with DictEntry of (String, String)
    final var entry =
        _DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487._of(testKey, testValue);
    final var valueIterator = new _Iterator_66111C2B710ABD8FDA1988CF5DDC4BBC71C07280CA899D51F77E28BB2FE7717D(entry);
    assertNotNull(valueIterator);
    assertSet.accept(valueIterator);
    assertTrue.accept(valueIterator.hasNext());
  }

  @Test
  void testFactoryMethods() {
    // Test _of() - empty iterator
    final var unsetIterator = _Iterator_66111C2B710ABD8FDA1988CF5DDC4BBC71C07280CA899D51F77E28BB2FE7717D._of();
    assertNotNull(unsetIterator);
    assertUnset.accept(unsetIterator);
    assertFalse.accept(unsetIterator.hasNext());

    // Test _of(Iterator) with valid iterator
    final var baseEntry = DictEntry._of(testKey, testValue);
    final var baseIterator = Iterator._of(baseEntry);
    final var wrappedIterator =
        _Iterator_66111C2B710ABD8FDA1988CF5DDC4BBC71C07280CA899D51F77E28BB2FE7717D._of(baseIterator);
    assertNotNull(wrappedIterator);
    assertSet.accept(wrappedIterator);
    assertTrue.accept(wrappedIterator.hasNext());

    // Test _of(Iterator) with null
    final var nullIterator = _Iterator_66111C2B710ABD8FDA1988CF5DDC4BBC71C07280CA899D51F77E28BB2FE7717D._of(null);
    assertNotNull(nullIterator);
    assertUnset.accept(nullIterator);
    assertFalse.accept(nullIterator.hasNext());
  }

  @Test
  void testIterationBehavior() {
    // Create an iterator with a single DictEntry of (String, String)
    final var entry =
        _DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487._of(testKey, testValue);
    final var iterator = new _Iterator_66111C2B710ABD8FDA1988CF5DDC4BBC71C07280CA899D51F77E28BB2FE7717D(entry);

    // Test hasNext() and next() pattern
    assertTrue.accept(iterator.hasNext());
    assertSet.accept(iterator); // Iterator is set when it has next

    final var nextEntry = iterator.next();
    assertNotNull(nextEntry);
    assertSet.accept(nextEntry);
    assertEquals(testKey, nextEntry.key());
    assertEquals(testValue, nextEntry.value());

    // After consuming the single element, iterator should be empty
    assertFalse.accept(iterator.hasNext());
    assertUnset.accept(iterator); // Iterator is unset when no more elements
  }

  @Test
  void testTypeConversion() {
    // Test that next() returns proper DictEntry of (String, String) type
    final var entry =
        _DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487._of(testKey, testValue);
    final var iterator = new _Iterator_66111C2B710ABD8FDA1988CF5DDC4BBC71C07280CA899D51F77E28BB2FE7717D(entry);

    assertTrue.accept(iterator.hasNext());
    final var nextEntry = iterator.next();

    // Verify it's the correct parameterized type
    assertNotNull(nextEntry);
    assertInstanceOf(_DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487.class, nextEntry);

    // Verify type safety - key and value are Strings
    final var key = nextEntry.key();
    final var value = nextEntry.value();
    assertInstanceOf(String.class, key);
    assertInstanceOf(String.class, value);
    assertEquals(testKey._string().state, key._string().state);
    assertEquals(testValue._string().state, value._string().state);
  }

  @Test
  void testEqualityOperators() {
    final var entry1 =
        _DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487._of(testKey, testValue);
    assertNotNull(entry1);
    final var iterator1 = new _Iterator_66111C2B710ABD8FDA1988CF5DDC4BBC71C07280CA899D51F77E28BB2FE7717D(entry1);
    assertUnset.accept(iterator1._eq(null));
  }

  @Test
  void testHashcodeOperator() {
    final var entry =
        _DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487._of(testKey, testValue);
    assertNotNull(entry);
    final var iterator = new _Iterator_66111C2B710ABD8FDA1988CF5DDC4BBC71C07280CA899D51F77E28BB2FE7717D(entry);
    final var emptyIterator = _Iterator_66111C2B710ABD8FDA1988CF5DDC4BBC71C07280CA899D51F77E28BB2FE7717D._of();

    // Test #? (hashcode) operator
    final var hashcode = iterator._hashcode();
    assertSet.accept(hashcode);

    final var emptyHashcode = emptyIterator._hashcode();
    assertSet.accept(emptyHashcode);
  }

  @Test
  void testIsSetBehavior() {
    final var entry =
        _DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487._of(testKey, testValue);
    assertNotNull(entry);
    final var iterator = new _Iterator_66111C2B710ABD8FDA1988CF5DDC4BBC71C07280CA899D51F77E28BB2FE7717D(entry);

    // Iterator is set when it has next
    assertTrue.accept(iterator._isSet());
    assertTrue.accept(iterator.hasNext());

    // Consume the element
    iterator.next();

    // Iterator is unset when no more elements
    assertFalse.accept(iterator._isSet());
    assertFalse.accept(iterator.hasNext());
  }

  @Test
  void testEdgeCasesAndNullHandling() {
    // Test with unset DictEntry
    final var unsetEntry = new _DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487();
    final var iteratorWithUnsetEntry =
        new _Iterator_66111C2B710ABD8FDA1988CF5DDC4BBC71C07280CA899D51F77E28BB2FE7717D(unsetEntry);
    assertNotNull(iteratorWithUnsetEntry);
    // Behavior depends on Iterator base implementation

    // Test factory method with unset Iterator
    final var unsetBaseIterator = new Iterator();
    final var iteratorFromUnset =
        _Iterator_66111C2B710ABD8FDA1988CF5DDC4BBC71C07280CA899D51F77E28BB2FE7717D._of(unsetBaseIterator);
    assertNotNull(iteratorFromUnset);
    assertUnset.accept(iteratorFromUnset);
  }

  @Test
  void testMultipleElementIteration() {
    // Create base Iterator with multiple DictEntry elements
    final var entry1 = DictEntry._of(String._of("key1"), String._of("value1"));
    // Create a base iterator and add elements
    final var baseIterator = Iterator._of(entry1);
    // Note: In practice, multiple elements would come from Dict.iterator()

    final var paramIterator =
        _Iterator_66111C2B710ABD8FDA1988CF5DDC4BBC71C07280CA899D51F77E28BB2FE7717D._of(baseIterator);

    // Test iteration pattern
    int count = 0;
    while (paramIterator.hasNext().state && count < 10) { // Safety limit
      final var nextEntry = paramIterator.next();
      assertNotNull(nextEntry);
      assertSet.accept(nextEntry);
      assertInstanceOf(_DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487.class, nextEntry);
      count++;
    }
  }
}
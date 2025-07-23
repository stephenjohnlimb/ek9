package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * I really do not want the overhead and complexity of mockito
 * adding agent and all that stuff, I want this light weight.
 * <p>
 * OK there is some local overhead and code duplication.
 * But I prefer that to having extra dependencies and weird agents.
 * </p>
 */
class OptionalTest extends Common {

  // Test data setup
  private final Optional unset = new Optional();
  private final String testValue = String._of("TestValue");
  private final Optional setOptional = new Optional(testValue);
  private final Integer testInt = Integer._of(42);
  private final Optional intOptional = new Optional(testInt);
  private final Boolean testBool = Boolean._of(true);
  private final Optional boolOptional = new Optional(testBool);

  // Helper methods for eliminating duplication
  
  /**
   * Helper method to assert that an Optional is unset and verify all related state.
   */
  private void assertOptionalUnset(Optional optional) {
    assertUnset.accept(optional);
    assertFalse.accept(optional._isSet());
    assertTrue.accept(optional._empty());
  }
  
  /**
   * Helper method to assert that an Optional is set and verify all related state.
   */
  private void assertOptionalSet(Optional optional) {
    assertSet.accept(optional);
    assertTrue.accept(optional._isSet());
    assertFalse.accept(optional._empty());
  }
  
  /**
   * Helper method to test whenPresent behavior with both Acceptor and Consumer.
   * Tests that the callbacks are called with the expected value.
   */
  private void assertWhenPresentCalled(Optional optional, Any expectedValue) {
    final var acceptor = new MockAcceptor();
    optional.whenPresent(acceptor);
    assertTrue(acceptor.verifyCalledWith(expectedValue));
    
    final var consumer = new MockConsumer();
    optional.whenPresent(consumer);
    assertTrue(consumer.verifyCalledWith(expectedValue));
  }
  
  /**
   * Helper method to test whenPresent behavior with both Acceptor and Consumer.
   * Tests that the callbacks are NOT called.
   */
  private void assertWhenPresentNotCalled(Optional optional) {
    final var acceptor = new MockAcceptor();
    optional.whenPresent(acceptor);
    assertTrue(acceptor.verifyNotCalled());
    
    final var consumer = new MockConsumer();
    optional.whenPresent(consumer);
    assertTrue(consumer.verifyNotCalled());
  }

  @Test
  void testConstruction() {
    // Default constructor should create unset Optional
    final var defaultConstructor = new Optional();
    assertNotNull(defaultConstructor);
    assertOptionalUnset(defaultConstructor);

    // Value constructor should create set Optional
    final var valueConstructor = new Optional(Boolean._of(true));
    assertOptionalSet(valueConstructor);

    // Factory method with no args should create unset Optional
    final var factoryEmpty = Optional._of();
    assertOptionalUnset(factoryEmpty);

    // Factory method with value should create set Optional
    final var factoryWithValue = Optional._of(String._of("test"));
    assertOptionalSet(factoryWithValue);

    // Null value should create unset Optional
    final var nullValue = new Optional(null);
    assertOptionalUnset(nullValue);

    // Factory method with null should create unset Optional
    final var factoryWithNull = Optional._of(null);
    assertOptionalUnset(factoryWithNull);
  }

  @Test
  void testStateManagement() {
    // Test state of unset Optional using helper
    assertOptionalUnset(unset);

    // Test state of set Optional using helper
    assertOptionalSet(setOptional);

    // Test state consistency for different value types using helper
    assertOptionalSet(intOptional);
    assertOptionalSet(boolOptional);

    // Test state after creating empty Optional
    final var emptyOptional = setOptional.asEmpty();
    assertNotNull(emptyOptional);
    assertOptionalUnset(emptyOptional);
  }

  @Test
  void testValueAccess() {
    // Test get() method - should throw exception on unset Optional
    assertThrows(Exception.class, unset::get);

    // Test get() method on set Optional (within safe block)
    if (setOptional._isSet().state) {
      final var value = setOptional.get();
      assertNotNull(value);
      assertEquals(testValue, value);
    }

    // Test getOrDefault on unset Optional
    final var defaultValue = String._of("default");
    final var resultFromUnset = unset.getOrDefault(defaultValue);
    assertEquals(defaultValue, resultFromUnset);

    // Test getOrDefault on set Optional
    final var resultFromSet = setOptional.getOrDefault(defaultValue);
    assertEquals(testValue, resultFromSet);

    // Test getOrDefault with different types
    final var intDefault = Integer._of(999);
    final var intResult = intOptional.getOrDefault(intDefault);
    assertEquals(testInt, intResult);

    final var boolDefault = Boolean._of(false);
    final var boolResult = boolOptional.getOrDefault(boolDefault);
    assertEquals(testBool, boolResult);

    // Test getOrDefault with null default
    final var nullDefault = unset.getOrDefault(null);
    assertNull(nullDefault);
  }

  @Test
  void testUtilityMethods() {
    // Test asEmpty() method
    final var emptyFromSet = setOptional.asEmpty();
    assertUnset.accept(emptyFromSet);
    assertFalse.accept(emptyFromSet._isSet());
    assertTrue.accept(emptyFromSet._empty());

    final var emptyFromUnset = unset.asEmpty();
    assertUnset.accept(emptyFromUnset);
    assertFalse.accept(emptyFromUnset._isSet());
    assertTrue.accept(emptyFromUnset._empty());

    // Test iterator() method on unset Optional
    final var unsetIterator = unset.iterator();
    assertFalse.accept(unsetIterator._isSet());
    assertFalse.accept(unsetIterator.hasNext());

    // Test iterator() method on set Optional
    final var setIterator = setOptional.iterator();
    assertTrue.accept(setIterator._isSet());
    assertTrue.accept(setIterator.hasNext());

    // Test whenPresent with set Optional using helper
    assertWhenPresentCalled(setOptional, testValue);

    // Test whenPresent with unset Optional using helper
    assertWhenPresentNotCalled(unset);
  }

  @Test
  void testOperators() {
    // Test _string operator ($) on unset Optional
    final var unsetString = unset._string();
    assertNotNull(unsetString);
    // Check that unset string is empty or unset
    final var isEmpty = unsetString.toString().isEmpty();
    final var isStringUnset = !unsetString._isSet().state;
    assertTrue(isEmpty || isStringUnset);

    // Test _string operator ($) on set Optional
    final var setString = setOptional._string();
    assertNotNull(setString);
    assertEquals(testValue, setString);

    // Test _hashcode operator (#?) on unset Optional
    final var unsetHash = unset._hashcode();
    assertUnset.accept(unsetHash);

    // Test _hashcode operator (#?) on set Optional
    final var setHash = setOptional._hashcode();
    assertSet.accept(setHash);
    assertNotNull(setHash);

    // Test _contains operator on unset Optional
    assertUnset.accept(unset._contains(testValue));
    assertUnset.accept(unset._contains(null));

    // Test _contains operator on set Optional
    assertTrue.accept(setOptional._contains(testValue));
    assertFalse.accept(setOptional._contains(String._of("different")));
    assertUnset.accept(setOptional._contains(null));

    // Test _contains with different types
    assertTrue.accept(intOptional._contains(testInt));
    assertFalse.accept(intOptional._contains(Integer._of(999)));

    assertTrue.accept(boolOptional._contains(testBool));
    assertFalse.accept(boolOptional._contains(Boolean._of(false)));
  }

  @Test
  void testAssignmentOperators() {
    // Test _copy operator (:=:)
    final var copyTarget = new Optional();
    copyTarget._copy(testValue);
    assertSet.accept(copyTarget);
    assertTrue.accept(copyTarget._isSet());
    assertEquals(testValue, copyTarget._string());

    // Test _copy with null (should result in unset)
    final var copyTargetNull = new Optional(String._of("initial"));
    copyTargetNull._copy(null);
    assertUnset.accept(copyTargetNull);
    assertFalse.accept(copyTargetNull._isSet());

    // Test _replace operator (:^:)
    final var replaceTarget = new Optional(String._of("old"));
    replaceTarget._replace(String._of("new"));
    assertSet.accept(replaceTarget);
    assertEquals(String._of("new"), replaceTarget._string());

    // Test _merge operator (:~:)
    final var mergeTarget = new Optional();
    mergeTarget._merge(Boolean._of(true));
    assertSet.accept(mergeTarget);

    // Test _pipe operator (|)
    final var pipeTarget = new Optional();
    pipeTarget._pipe(testValue);
    assertSet.accept(pipeTarget);
    assertEquals(testValue, pipeTarget._string());

    // Test assignment operators with unset values
    final var unsetTarget = new Optional(String._of("initial"));
    unsetTarget._copy(null);
    assertUnset.accept(unsetTarget);
  }

  @Test
  void testEdgeCases() {
    // Test operations on unset Optional don't throw exceptions
    assertFalse.accept(unset._isSet());
    assertTrue.accept(unset._empty());
    assertUnset.accept(unset.asEmpty());

    // Test toString on unset Optional
    assertNotNull(unset.toString());

    // Test toString on set Optional
    assertNotNull(setOptional.toString());

    // Test equals on unset Optionals
    final var anotherUnset = new Optional();
    assertUnset.accept(anotherUnset);

    // Test equals on set Optionals with same value
    final var anotherSet = new Optional(testValue);
    assertEquals(setOptional, anotherSet);

    // Test equals on set Optionals with different values
    final var differentSet = new Optional(String._of("different"));
    assertNotEquals(setOptional, differentSet);

    // Test hashCode consistency
    assertUnset.accept(anotherUnset._hashcode());
    assertSet.accept(setOptional._hashcode());

    // Test mixed type operations, the answer is unset because we should not be
    //mixing incompatible types
    final var mixedOptional = new Optional(Integer._of(123));
    assertUnset.accept(mixedOptional._contains(String._of("123")));
    //Also unset result because it is holding a String type.
    assertUnset.accept(setOptional._contains(Integer._of(123)));
  }

  @Test
  void testIteratorIntegration() {
    // Test iterator with while loop on unset Optional
    int unsetCount = 0;
    final var unsetIter = unset.iterator();
    while (unsetIter.hasNext().state) {
      unsetIter.next();
      unsetCount++;
    }
    assertEquals(0, unsetCount);

    // Test iterator with while loop on set Optional
    int setCount = 0;
    final var setIter = setOptional.iterator();
    while (setIter.hasNext().state) {
      final var value = setIter.next();
      assertNotNull(value);
      setCount++;
    }
    assertEquals(1, setCount);

    // Test iterator state
    final var iter1 = setOptional.iterator();
    final var iter2 = setOptional.iterator();
    assertTrue.accept(iter1._isSet());
    assertTrue.accept(iter2._isSet());

    // Both should return the same value
    assertEquals(iter1.next(), iter2.next());
    assertFalse.accept(iter1._isSet());
    assertFalse.accept(iter2._isSet());
  }

  @Test
  void testCollectionIntegration() {
    // Test Optional behavior consistent with built-in types
    final var optionalOfInteger = new Optional(Integer._of(42));
    final var optionalOfString = new Optional(String._of("test"));
    final var optionalOfBoolean = new Optional(Boolean._of(true));

    // All should be set using helper
    assertOptionalSet(optionalOfInteger);
    assertOptionalSet(optionalOfString);
    assertOptionalSet(optionalOfBoolean);

    // Test Optional with unset built-in types
    final var unsetInt = new Integer();
    assertNotNull(unsetInt);
    final var optionalOfUnsetInt = new Optional(unsetInt);

    //Now because the item in the optional is unset, then the Optional itself is unset.
    //Not this is different to collection types like Lists and dictionaries.
    assertOptionalUnset(optionalOfUnsetInt);

  }

  @Test
  void testWhenNotPresent() {

    //I've used 'any' here but used a String
    final var any = String._of("Test Value");
    final var asUnset = new Optional(any);

    // Test whenPresent called with set Optional using helper
    assertWhenPresentCalled(asUnset, any);
  }

  @Test
  void testWhenPresent() {

    final var asUnset = new Optional();

    // Test whenPresent not called with unset Optional using helper
    assertWhenPresentNotCalled(asUnset);
  }

}
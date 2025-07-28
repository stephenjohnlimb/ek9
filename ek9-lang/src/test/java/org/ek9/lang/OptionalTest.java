package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
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
  private final Optional unsetOptional = new Optional();
  private final String testValue = String._of("TestValue");
  private final Optional setOptional = new Optional(testValue);
  private final Integer testInt = INT_42;
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
  
  /**
   * Helper method to test contains behavior with expected and unexpected values.
   * Tests both positive and negative contains cases.
   */
  private void assertContainsBehavior(Optional optional, Any expectedValue, Any unexpectedValue) {
    assertTrue.accept(optional._contains(expectedValue));
    assertFalse.accept(optional._contains(unexpectedValue));
    assertUnset.accept(optional._contains(null));
  }
  
  /**
   * Helper method to test assignment operators (copy, replace, merge).
   * Tests that the operator correctly assigns from source to target.
   */
  private void assertAssignmentOperator(java.util.function.BiConsumer<Optional, Optional> operator, 
                                       Optional source, Any expectedValue) {
    final var target = new Optional();
    operator.accept(target, source);
    assertOptionalSet(target);
    assertEquals(expectedValue, target._string());
  }
  
  /**
   * Helper method to assert that an Optional created via asEmpty() has proper empty state.
   */
  private void assertEmptyOptionalState(Optional optional) {
    final var emptyOptional = optional.asEmpty();
    assertNotNull(emptyOptional);
    assertOptionalUnset(emptyOptional);
  }

  @Nested
  class Construction {
    
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
      assertOptionalUnset(unsetOptional);

      // Test state of set Optional using helper
      assertOptionalSet(setOptional);

      // Test state consistency for different value types using helper
      assertOptionalSet(intOptional);
      assertOptionalSet(boolOptional);

      // Test state after creating empty Optional
      assertEmptyOptionalState(setOptional);
    }
  }

  @Nested
  class ValueAccess {
    
    @Test
    void testValueAccess() {
      // Test get() method - should throw exception on unset Optional
      assertThrows(Exception.class, unsetOptional::get);

      // Test get() method on set Optional (within safe block)
      if (setOptional._isSet().state) {
        final var value = setOptional.get();
        assertNotNull(value);
        assertEquals(testValue, value);
      }

      // Test getOrDefault on unset Optional
      final var defaultValue = STR_DEFAULT;
      final var resultFromUnset = unsetOptional.getOrDefault(defaultValue);
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
      final var nullDefault = unsetOptional.getOrDefault(null);
      assertNull(nullDefault);
    }
  }

  @Nested
  class UtilityMethods {
    
    @Test
    void testUtilityMethods() {
      // Test asEmpty() method
      assertEmptyOptionalState(setOptional);
      assertEmptyOptionalState(unsetOptional);

      // Test iterator() method on unset Optional
      final var unsetIterator = unsetOptional.iterator();
      assertFalse.accept(unsetIterator._isSet());
      assertFalse.accept(unsetIterator.hasNext());

      // Test iterator() method on set Optional
      final var setIterator = setOptional.iterator();
      assertTrue.accept(setIterator._isSet());
      assertTrue.accept(setIterator.hasNext());

      // Test whenPresent with set Optional using helper
      assertWhenPresentCalled(setOptional, testValue);

      // Test whenPresent with unset Optional using helper
      assertWhenPresentNotCalled(unsetOptional);
    }
  }

  @Nested
  class Operators {
    
    @Test
    void testOperators() {
      // Test _string operator ($) on unset Optional
      final var unsetString = unsetOptional._string();
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
      final var unsetHash = unsetOptional._hashcode();
      assertUnset.accept(unsetHash);

      // Test _hashcode operator (#?) on set Optional
      final var setHash = setOptional._hashcode();
      assertSet.accept(setHash);
      assertNotNull(setHash);

      // Test _contains operator on unset Optional
      assertUnset.accept(unsetOptional._contains(testValue));
      assertUnset.accept(unsetOptional._contains(null));

      // Test _contains operator on set Optional
      assertContainsBehavior(setOptional, testValue, String._of("different"));

      // Test _contains with different types
      assertContainsBehavior(intOptional, testInt, Integer._of(999));
      assertContainsBehavior(boolOptional, testBool, Boolean._of(false));

      //Same values but different instances.
      final var anotherOptional = Optional._of(testValue);
      final var checkOptional = Optional._of( String._of("TestValue"));
      final var differentOptional = Optional._of( String._of("DifferentValue"));

      final Any viaAny = checkOptional;

      assertTrue.accept(anotherOptional._eq(checkOptional));
      assertTrue.accept(anotherOptional._eq(viaAny));
      assertTrue.accept(anotherOptional._neq(differentOptional));

      assertUnset.accept(anotherOptional._eq(unsetOptional));
      assertUnset.accept(anotherOptional._eq(new Any(){}));
    }
  }

  @Nested
  class AssignmentOperators {
    
    @Test
    void testAssignmentOperators() {
      // Test assignment operators with set values
      assertAssignmentOperator(Optional::_copy, setOptional, testValue);
      assertAssignmentOperator(Optional::_replace, Optional._of(String._of("new")), String._of("new"));  
      assertAssignmentOperator(Optional::_merge, Optional._of(Boolean._of(true)), Boolean._of(true)._string());

      // Test _copy with null (should result in unset)
      final var copyTargetNull = new Optional(String._of("initial"));
      copyTargetNull._copy(null);
      assertOptionalUnset(copyTargetNull);

      // Test _pipe operator (|)
      final var pipeTarget = new Optional();
      pipeTarget._pipe(testValue);
      assertOptionalSet(pipeTarget);
      assertEquals(testValue, pipeTarget._string());

      // Test assignment operators with unset values
      final var unsetTarget = new Optional(String._of("initial"));
      unsetTarget._copy(null);
      assertOptionalUnset(unsetTarget);
    }
  }

  @Nested
  class EdgeCases {
    
    @Test
    void testEdgeCases() {
      // Test safe operations on unset Optional (no exceptions thrown)
      assertOptionalUnset(unsetOptional);
      assertEmptyOptionalState(unsetOptional);
      assertNotNull(unsetOptional.toString());

      // Test toString and equality for set Optional  
      assertNotNull(setOptional.toString());
      assertEquals(setOptional, new Optional(testValue)); // Same value
      assertNotEquals(setOptional, new Optional(String._of("different"))); // Different value

      // Test hashCode consistency with state
      assertUnset.accept(new Optional()._hashcode()); // Unset -> unset hashcode
      assertSet.accept(setOptional._hashcode()); // Set -> set hashcode

      // Test type safety: mixed type operations return unset (no type confusion)
      final var optionalOfInteger = new Optional(Integer._of(123));
      assertUnset.accept(optionalOfInteger._contains(String._of("123"))); // Int Optional doesn't contain String
      assertUnset.accept(setOptional._contains(Integer._of(123))); // String Optional doesn't contain Int
    }
  }

  @Nested
  class Integration {
    
    @Test
    void testIteratorIntegration() {
      // Test iterator behavior: unset Optional has no iterations
      int unsetCount = 0;
      final var unsetIter = unsetOptional.iterator();
      while (unsetIter.hasNext().state) {
        unsetIter.next();
        unsetCount++;
      }
      assertEquals(0, unsetCount);

      // Test iterator behavior: set Optional has exactly one iteration
      int setCount = 0;
      final var setIter = setOptional.iterator();
      while (setIter.hasNext().state) {
        final var value = setIter.next();
        assertEquals(testValue, value);
        setCount++;
      }
      assertEquals(1, setCount);

      // Test iterator state transitions: multiple iterators are independent
      final var iter1 = setOptional.iterator();
      final var iter2 = setOptional.iterator();
      assertEquals(iter1.next(), iter2.next()); // Both return same value
      assertFalse.accept(iter1._isSet()); // Both become unset after use
      assertFalse.accept(iter2._isSet());
    }

    @Test
    void testCollectionIntegration() {
      // Test Optional behavior consistent with built-in types
      assertOptionalSet(new Optional(INT_42));
      assertOptionalSet(new Optional(String._of("test")));
      assertOptionalSet(new Optional(Boolean._of(true)));

      // Test Optional with unset built-in types - key difference from collections
      // When Optional contains unset value, the Optional itself becomes unset
      // (Unlike Lists/Dicts which remain set even when empty)
      final var optionalOfUnsetInt = new Optional(new Integer());
      assertOptionalUnset(optionalOfUnsetInt);
    }

  }

}
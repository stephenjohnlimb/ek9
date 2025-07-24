package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Test Consumer of JSON parameterized function type.
 * This tests the delegation pattern and JSON-specific type safety.
 * Consumer is marked as pure in EK9, maintaining that semantic.
 */
@SuppressWarnings({"checkstyle:MethodName", "checkstyle:AbbreviationAsWordInName", "checkstyle:TypeName"})
class _Consumer_376C0B739AC00DD9FA60B1DE33853E8AA9F76AF08832303EB1027CCB027B8588Test extends Common {

  // Type alias for the long parameterized Consumer class name
  private static final Class<_Consumer_376C0B739AC00DD9FA60B1DE33853E8AA9F76AF08832303EB1027CCB027B8588> CONSUMER_JSON = 
      _Consumer_376C0B739AC00DD9FA60B1DE33853E8AA9F76AF08832303EB1027CCB027B8588.class;
  
  // Factory methods for cleaner test code
  private static _Consumer_376C0B739AC00DD9FA60B1DE33853E8AA9F76AF08832303EB1027CCB027B8588 consumerJSON() {
    return new _Consumer_376C0B739AC00DD9FA60B1DE33853E8AA9F76AF08832303EB1027CCB027B8588();
  }
  
  // Helper methods for reducing assertion duplication
  
  /**
   * Helper method to assert that a Consumer&lt;JSON&gt; is set and verify all related state.
   */
  private void assertFunctionSet(_Consumer_376C0B739AC00DD9FA60B1DE33853E8AA9F76AF08832303EB1027CCB027B8588 consumer) {
    assertSet.accept(consumer);
    assertTrue.accept(consumer._isSet());
  }

  @Test
  void testConstructionAndFactoryMethods() {
    // Test default constructor
    assertFunctionSet(consumerJSON());

    // Test factory method with no arguments
    assertFunctionSet(_Consumer_376C0B739AC00DD9FA60B1DE33853E8AA9F76AF08832303EB1027CCB027B8588._of());

    // Test factory method with base Consumer
    assertFunctionSet(_Consumer_376C0B739AC00DD9FA60B1DE33853E8AA9F76AF08832303EB1027CCB027B8588._of(new Consumer()));

    // Test factory method with null Consumer
    assertFunctionSet(_Consumer_376C0B739AC00DD9FA60B1DE33853E8AA9F76AF08832303EB1027CCB027B8588._of(null));
  }

  @Test
  void testJSONParameterCall() {
    // Create a concrete implementation for testing
    final var testConsumer = new _Consumer_376C0B739AC00DD9FA60B1DE33853E8AA9F76AF08832303EB1027CCB027B8588() {
      private JSON lastCalled = null;

      @Override
      public void _call(JSON t) {
        super._call(t); // Call delegate
        this.lastCalled = t; // Track for verification
      }

      public JSON getLastCalled() {
        return lastCalled;
      }
    };

    // Test JSON parameter acceptance with different values
    final var stringJSON = JSON._of("\"test string\"");
    testConsumer._call(stringJSON);
    assertEquals(stringJSON, testConsumer.getLastCalled());

    final var numberJSON = JSON._of("42");
    testConsumer._call(numberJSON);
    assertEquals(numberJSON, testConsumer.getLastCalled());

    final var objectJSON = JSON._of("{\"key\": \"value\"}");
    testConsumer._call(objectJSON);
    assertEquals(objectJSON, testConsumer.getLastCalled());

    final var arrayJSON = JSON._of("[1, 2, 3]");
    testConsumer._call(arrayJSON);
    assertEquals(arrayJSON, testConsumer.getLastCalled());
    
    // Test pure semantics - Consumer is marked as pure in EK9
    final var consumer = consumerJSON();
    assertDoesNotThrow(() -> consumer._call(JSON._of("\"PureTest\"")));
    assertFunctionSet(consumer); // Consumer state should remain consistent
  }

  @Test
  void testEqualityOperators() {
    final var consumer1 = consumerJSON();
    final var consumer2 = consumerJSON();

    // Test basic equality functionality - implementation details may vary
    assertNotNull(consumer1._eq(consumer2));
    assertNotNull(consumer1._eq(consumer1));

    // Test _eq with Any (polymorphic)
    assertNotNull(consumer1._eq((Any) consumer2));
    assertNotNull(consumer1._eq((Any) consumer1));

    // Test _eq with null and different type
    assertUnset.accept(consumer1._eq(null));
    assertUnset.accept(consumer1._eq(String._of("test")));
  }

  @Test
  void testOperatorConsistencyAndTypeSafety() {
    final var consumer = consumerJSON();

    // Test operator consistency - Functions should have consistent set/unset behavior
    assertNotNull(consumer._isSet());
    assertNotNull(consumer._string());
    assertNotNull(consumer._hashcode());

    // Verify class type is correct
    assertEquals(CONSUMER_JSON, consumer.getClass());

    // Test delegation produces consistent behavior with base type
    assertEquals(new Consumer()._isSet(), consumer._isSet());
  }

}
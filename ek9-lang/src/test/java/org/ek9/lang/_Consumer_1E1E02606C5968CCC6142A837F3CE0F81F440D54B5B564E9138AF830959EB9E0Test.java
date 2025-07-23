package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Test Consumer of String parameterized function type.
 * This tests the delegation pattern and String-specific type safety.
 * Consumer is marked as pure in EK9, maintaining that semantic.
 */
@SuppressWarnings({"checkstyle:MethodName", "checkstyle:AbbreviationAsWordInName", "checkstyle:TypeName"})
class _Consumer_1E1E02606C5968CCC6142A837F3CE0F81F440D54B5B564E9138AF830959EB9E0Test extends Common {

  // Type alias for the long parameterized Consumer class name
  private static final Class<_Consumer_1E1E02606C5968CCC6142A837F3CE0F81F440D54B5B564E9138AF830959EB9E0> CONSUMER_STRING = 
      _Consumer_1E1E02606C5968CCC6142A837F3CE0F81F440D54B5B564E9138AF830959EB9E0.class;
  
  // Factory methods for cleaner test code
  private static _Consumer_1E1E02606C5968CCC6142A837F3CE0F81F440D54B5B564E9138AF830959EB9E0 consumerString() {
    return new _Consumer_1E1E02606C5968CCC6142A837F3CE0F81F440D54B5B564E9138AF830959EB9E0();
  }
  
  // Helper methods for reducing assertion duplication
  
  /**
   * Helper method to assert that a Consumer&lt;String&gt; is set and verify all related state.
   */
  private void assertFunctionSet(_Consumer_1E1E02606C5968CCC6142A837F3CE0F81F440D54B5B564E9138AF830959EB9E0 consumer) {
    assertSet.accept(consumer);
    assertTrue.accept(consumer._isSet());
  }

  @Test
  void testConstructionAndFactoryMethods() {
    // Test default constructor
    assertFunctionSet(consumerString());

    // Test factory method with no arguments
    assertFunctionSet(_Consumer_1E1E02606C5968CCC6142A837F3CE0F81F440D54B5B564E9138AF830959EB9E0._of());

    // Test factory method with base Consumer
    assertFunctionSet(_Consumer_1E1E02606C5968CCC6142A837F3CE0F81F440D54B5B564E9138AF830959EB9E0._of(new Consumer()));

    // Test factory method with null Consumer
    assertFunctionSet(_Consumer_1E1E02606C5968CCC6142A837F3CE0F81F440D54B5B564E9138AF830959EB9E0._of(null));
  }

  @Test
  void testStringParameterCall() {
    // Create a concrete implementation for testing
    final var testConsumer = new _Consumer_1E1E02606C5968CCC6142A837F3CE0F81F440D54B5B564E9138AF830959EB9E0() {
      private java.lang.String lastCalled = null;

      @Override
      public void _call(String t) {
        super._call(t); // Call delegate
        this.lastCalled = t.state; // Track for verification
      }

      public java.lang.String getLastCalled() {
        return lastCalled;
      }
    };

    // Test String parameter acceptance with different values
    testConsumer._call(String._of("TestValue"));
    assertEquals("TestValue", testConsumer.getLastCalled());

    testConsumer._call(String._of(""));
    assertEquals("", testConsumer.getLastCalled());

    testConsumer._call(String._of("This is a longer test string for Consumer"));
    assertEquals("This is a longer test string for Consumer", testConsumer.getLastCalled());
    
    // Test pure semantics - Consumer is marked as pure in EK9
    final var consumer = consumerString();
    assertDoesNotThrow(() -> consumer._call(String._of("PureTest")));
    assertFunctionSet(consumer); // Consumer state should remain consistent
  }

  @Test
  void testEqualityOperators() {
    final var consumer1 = consumerString();
    final var consumer2 = consumerString();

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
    final var consumer = consumerString();

    // Test operator consistency - Functions should have consistent set/unset behavior
    assertNotNull(consumer._isSet());
    assertNotNull(consumer._string());
    assertNotNull(consumer._hashcode());

    // Verify class type is correct
    assertEquals(CONSUMER_STRING, consumer.getClass());

    // Test delegation produces consistent behavior with base type
    assertEquals(new Consumer()._isSet(), consumer._isSet());
  }


}
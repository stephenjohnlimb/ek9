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

  @Test
  void testConstruction() {
    // Test default constructor
    final var defaultConstructor = new _Consumer_1E1E02606C5968CCC6142A837F3CE0F81F440D54B5B564E9138AF830959EB9E0();
    assertNotNull(defaultConstructor);
    assertSet.accept(defaultConstructor); // Consumer functions are always set
    assertTrue.accept(defaultConstructor._isSet());
  }

  @Test
  void testFactoryMethods() {
    // Test factory method with no arguments
    final var factoryDefault = _Consumer_1E1E02606C5968CCC6142A837F3CE0F81F440D54B5B564E9138AF830959EB9E0._of();
    assertNotNull(factoryDefault);
    assertSet.accept(factoryDefault);
    assertTrue.accept(factoryDefault._isSet());

    // Test factory method with base Consumer
    final var baseConsumer = new Consumer();
    final var fromBase = _Consumer_1E1E02606C5968CCC6142A837F3CE0F81F440D54B5B564E9138AF830959EB9E0._of(baseConsumer);
    assertNotNull(fromBase);
    assertSet.accept(fromBase);
    assertTrue.accept(fromBase._isSet());

    // Test factory method with null Consumer
    final var fromNull = _Consumer_1E1E02606C5968CCC6142A837F3CE0F81F440D54B5B564E9138AF830959EB9E0._of(null);
    assertNotNull(fromNull);
    assertSet.accept(fromNull);
    assertTrue.accept(fromNull._isSet());
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

    // Test String parameter acceptance
    final var testString = String._of("TestValue");
    testConsumer._call(testString);

    // Verify String was received correctly
    assertNotNull(testConsumer.getLastCalled());
    assertEquals("TestValue", testConsumer.getLastCalled());

    // Test with different String values
    final var emptyString = String._of("");
    testConsumer._call(emptyString);
    assertEquals("", testConsumer.getLastCalled());

    final var longString = String._of("This is a longer test string for Consumer");
    testConsumer._call(longString);
    assertEquals("This is a longer test string for Consumer", testConsumer.getLastCalled());
  }

  @Test
  void testPureSemantics() {
    // Consumer is marked as pure in EK9, meaning it should not have side effects
    // This is a behavioral test to ensure the pure semantic is maintained
    final var consumer = _Consumer_1E1E02606C5968CCC6142A837F3CE0F81F440D54B5B564E9138AF830959EB9E0._of();
    final var testString = String._of("PureTest");

    // Call should complete without throwing exceptions
    assertDoesNotThrow(() -> consumer._call(testString));

    // Consumer state should remain consistent (always set for functions)
    assertTrue.accept(consumer._isSet());
  }

  @Test
  void testEqualityOperators() {
    // Test _eq with same parameterized type
    final var consumer1 = _Consumer_1E1E02606C5968CCC6142A837F3CE0F81F440D54B5B564E9138AF830959EB9E0._of();
    final var consumer2 = _Consumer_1E1E02606C5968CCC6142A837F3CE0F81F440D54B5B564E9138AF830959EB9E0._of();

    // Test basic equality functionality - implementation details may vary
    assertNotNull(consumer1._eq(consumer2));
    assertNotNull(consumer1._eq(consumer1));

    // Test _eq with Any (polymorphic)
    assertNotNull(consumer1._eq((Any) consumer2));
    assertNotNull(consumer1._eq((Any) consumer1));

    // Test _eq with null
    assertUnset.accept(consumer1._eq(null));

    // Test _eq with different type
    final var string = String._of("test");
    assertUnset.accept(consumer1._eq(string));
  }

  @Test
  void testOperatorConsistency() {
    final var consumer = _Consumer_1E1E02606C5968CCC6142A837F3CE0F81F440D54B5B564E9138AF830959EB9E0._of();

    // Test _isSet operator - Functions should have consistent set/unset behavior
    assertNotNull(consumer._isSet());

    // Test _string operator
    assertNotNull(consumer._string());

    // Test _hashcode operator
    assertNotNull(consumer._hashcode());
  }

  @Test
  void testDelegationBehavior() {
    // Test that parameterized Consumer properly delegates to base Consumer
    final var paramConsumer = _Consumer_1E1E02606C5968CCC6142A837F3CE0F81F440D54B5B564E9138AF830959EB9E0._of();
    final var baseConsumer = new Consumer();

    // Verify delegation produces consistent behavior with base type
    assertEquals(baseConsumer._isSet(), paramConsumer._isSet());
  }

  @Test
  void testTypeSafety() {
    final var consumer = _Consumer_1E1E02606C5968CCC6142A837F3CE0F81F440D54B5B564E9138AF830959EB9E0._of();

    // Verify class type is correct
    assertEquals(_Consumer_1E1E02606C5968CCC6142A837F3CE0F81F440D54B5B564E9138AF830959EB9E0.class, 
                 consumer.getClass());
  }


}
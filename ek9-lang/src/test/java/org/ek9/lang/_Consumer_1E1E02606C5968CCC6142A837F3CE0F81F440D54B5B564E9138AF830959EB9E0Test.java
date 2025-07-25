package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Test Consumer of String parameterized function type.
 * This tests the delegation pattern and String-specific type safety.
 * Consumer is marked as pure in EK9, maintaining that semantic.
 */
@SuppressWarnings({"checkstyle:MethodName", "checkstyle:AbbreviationAsWordInName", "checkstyle:TypeName"})
class _Consumer_1E1E02606C5968CCC6142A837F3CE0F81F440D54B5B564E9138AF830959EB9E0Test extends Common {

  private static _Consumer_1E1E02606C5968CCC6142A837F3CE0F81F440D54B5B564E9138AF830959EB9E0 consumerString() {
    return _Consumer_1E1E02606C5968CCC6142A837F3CE0F81F440D54B5B564E9138AF830959EB9E0._of();
  }

  private static _Consumer_1E1E02606C5968CCC6142A837F3CE0F81F440D54B5B564E9138AF830959EB9E0 consumerString(
      Consumer value) {
    return _Consumer_1E1E02606C5968CCC6142A837F3CE0F81F440D54B5B564E9138AF830959EB9E0._of(value);
  }


  @Test
  void testConstruction() {

    // Test factory method
    final var factoryConsumer = consumerString();
    assertNotNull(factoryConsumer);
    assertSet.accept(factoryConsumer);

    // Test factory method with base Consumer
    final var baseConsumer = new Consumer();
    final var fromBase = consumerString(baseConsumer);
    assertNotNull(fromBase);
    assertSet.accept(fromBase);

    // Test factory method with null
    final var fromNull = consumerString(null);
    assertNotNull(fromNull);
    assertSet.accept(fromNull);
  }

  @Test
  void testFunctionCall() {
    final var consumer = consumerString();
    assertNotNull(consumer);

    // Test call with String - should not throw exception
    final var testString = String._of("test");
    consumer._call(testString);

    final var emptyString = String._of("");
    consumer._call(emptyString);

    // Test call with null String - should not throw exception
    consumer._call(null);
  }

  @Test
  void testEquality() {
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
  void testStringAndHashOperators() {
    final var consumer = consumerString();

    // Test operator consistency - Functions should have consistent set/unset behavior
    assertNotNull(consumer._isSet());
    assertNotNull(consumer._string());
    assertNotNull(consumer._hashcode());
  }

  @Test
  void testDelegationConsistency() {
    // Test delegation produces consistent behavior with base type
    final var consumer = consumerString();
    final var baseConsumer = new Consumer();

    // Both should be set and functional
    assertTrue.accept(consumer._isSet());
    assertTrue.accept(baseConsumer._isSet());
    assertNotNull(consumer._string());
    assertNotNull(baseConsumer._string());
  }


}
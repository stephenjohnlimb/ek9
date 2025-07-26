package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Test Consumer of JSON parameterized function type.
 * This tests the delegation pattern and JSON-specific type safety.
 * Consumer is marked as pure in EK9, maintaining that semantic.
 */
@SuppressWarnings({"checkstyle:MethodName", "checkstyle:AbbreviationAsWordInName", "checkstyle:TypeName"})
class _Consumer_376C0B739AC00DD9FA60B1DE33853E8AA9F76AF08832303EB1027CCB027B8588Test extends Common {

  private static _Consumer_376C0B739AC00DD9FA60B1DE33853E8AA9F76AF08832303EB1027CCB027B8588 consumerJSON() {
    return _Consumer_376C0B739AC00DD9FA60B1DE33853E8AA9F76AF08832303EB1027CCB027B8588._of();
  }

  private static _Consumer_376C0B739AC00DD9FA60B1DE33853E8AA9F76AF08832303EB1027CCB027B8588 consumerJSON(
      Consumer value) {
    return _Consumer_376C0B739AC00DD9FA60B1DE33853E8AA9F76AF08832303EB1027CCB027B8588._of(value);
  }


  @Test
  void testConstruction() {

    // Test factory method
    final var factoryConsumer = consumerJSON();
    assertNotNull(factoryConsumer);
    assertSet.accept(factoryConsumer);

    // Test factory method with base Consumer
    final var baseConsumer = new Consumer();
    final var fromBase = consumerJSON(baseConsumer);
    assertNotNull(fromBase);
    assertSet.accept(fromBase);

    // Test factory method with null
    final var fromNull = consumerJSON(null);
    assertNotNull(fromNull);
    assertSet.accept(fromNull);
  }

  @Test
  void testFunctionCall() {
    final var consumer = consumerJSON();
    assertNotNull(consumer);

    // Test call with JSON - should not throw exception
    final var testJSON = JSON._of("{\"test\": \"value\"}");
    consumer._call(testJSON);

    consumer._call(JSON_42);

    // Test call with null JSON - should not throw exception
    consumer._call(null);
  }

  @Test
  void testEquality() {
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
  void testStringAndHashOperators() {
    final var consumer = consumerJSON();

    // Test operator consistency - Functions should have consistent set/unset behavior
    assertNotNull(consumer._isSet());
    assertNotNull(consumer._string());
    assertNotNull(consumer._hashcode());
  }

  @Test
  void testDelegationConsistency() {
    // Test delegation produces consistent behavior with base type
    final var consumer = consumerJSON();
    final var baseConsumer = new Consumer();

    // Both should be set and functional
    assertTrue.accept(consumer._isSet());
    assertTrue.accept(baseConsumer._isSet());
    assertNotNull(consumer._string());
    assertNotNull(baseConsumer._string());
  }

}
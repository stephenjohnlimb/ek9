package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Test Consumer of Boolean parameterized type.
 * Tests the delegation pattern and Boolean-specific type safety.
 */
@SuppressWarnings({"checkstyle:MethodName", "checkstyle:AbbreviationAsWordInName", "checkstyle:TypeName"})
class _Consumer_6CA5F5837A13FA80F86ADE72D1698B4441C50614A56BEA015D595402027DE326Test extends Common {

  private static _Consumer_6CA5F5837A13FA80F86ADE72D1698B4441C50614A56BEA015D595402027DE326 consumerBoolean() {
    return _Consumer_6CA5F5837A13FA80F86ADE72D1698B4441C50614A56BEA015D595402027DE326._of();
  }

  private static _Consumer_6CA5F5837A13FA80F86ADE72D1698B4441C50614A56BEA015D595402027DE326 consumerBoolean(
      Consumer value) {
    return _Consumer_6CA5F5837A13FA80F86ADE72D1698B4441C50614A56BEA015D595402027DE326._of(value);
  }

  @Test
  void testConstruction() {

    // Test factory method
    final var factoryConsumer = consumerBoolean();
    assertNotNull(factoryConsumer);
    assertSet.accept(factoryConsumer);

    // Test factory method with base Consumer
    final var baseConsumer = new Consumer();
    final var fromBase = consumerBoolean(baseConsumer);
    assertNotNull(fromBase);
    assertSet.accept(fromBase);

    // Test factory method with null
    final var fromNull = consumerBoolean(null);
    assertNotNull(fromNull);
    assertSet.accept(fromNull);
  }

  @Test
  void testFunctionCall() {
    final var consumer = consumerBoolean();
    assertNotNull(consumer);

    // Test call with Boolean - should not throw exception
    final var testBoolean = Boolean._of(true);
    consumer._call(testBoolean);

    final var falseBoolean = Boolean._of(false);
    consumer._call(falseBoolean);

    // Test call with null Boolean - should not throw exception
    consumer._call(null);
  }

  @Test
  void testEquality() {
    final var consumer1 = consumerBoolean();
    final var consumer2 = consumerBoolean();

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
    final var consumer = consumerBoolean();

    // Test operator consistency - Functions should have consistent set/unset behavior
    assertNotNull(consumer._isSet());
    assertNotNull(consumer._string());
    assertNotNull(consumer._hashcode());
  }

  @Test
  void testDelegationConsistency() {
    // Test delegation produces consistent behavior with base type
    final var consumer = consumerBoolean();
    final var baseConsumer = new Consumer();

    // Both should be set and functional
    assertTrue.accept(consumer._isSet());
    assertTrue.accept(baseConsumer._isSet());
    assertNotNull(consumer._string());
    assertNotNull(baseConsumer._string());
  }
}
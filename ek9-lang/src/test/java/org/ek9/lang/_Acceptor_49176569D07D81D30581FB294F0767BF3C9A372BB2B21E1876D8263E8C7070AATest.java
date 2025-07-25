package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Test Acceptor of String parameterized function type.
 * This tests the delegation pattern and String-specific type safety.
 */
@SuppressWarnings({"checkstyle:MethodName", "checkstyle:AbbreviationAsWordInName", "checkstyle:TypeName"})
class _Acceptor_49176569D07D81D30581FB294F0767BF3C9A372BB2B21E1876D8263E8C7070AATest extends Common {

  private static _Acceptor_49176569D07D81D30581FB294F0767BF3C9A372BB2B21E1876D8263E8C7070AA acceptorString() {
    return _Acceptor_49176569D07D81D30581FB294F0767BF3C9A372BB2B21E1876D8263E8C7070AA._of();
  }

  private static _Acceptor_49176569D07D81D30581FB294F0767BF3C9A372BB2B21E1876D8263E8C7070AA acceptorString(
      Acceptor value) {
    return _Acceptor_49176569D07D81D30581FB294F0767BF3C9A372BB2B21E1876D8263E8C7070AA._of(value);
  }

  // Helper methods for reducing assertion duplication


  @Test
  void testConstruction() {

    // Test factory method
    final var factoryAcceptor = acceptorString();
    assertNotNull(factoryAcceptor);
    assertSet.accept(factoryAcceptor);

    // Test factory method with base Acceptor
    final var baseAcceptor = new Acceptor();
    final var fromBase = acceptorString(baseAcceptor);
    assertNotNull(fromBase);
    assertSet.accept(fromBase);

    // Test factory method with null
    final var fromNull = acceptorString(null);
    assertNotNull(fromNull);
    assertSet.accept(fromNull);
  }

  @Test
  void testFunctionCall() {
    final var acceptor = acceptorString();
    assertNotNull(acceptor);

    // Test call with String - should not throw exception
    final var testString = String._of("test");
    acceptor._call(testString);

    final var emptyString = String._of("");
    acceptor._call(emptyString);

    // Test call with null String - should not throw exception
    acceptor._call(null);
  }

  @Test
  void testEquality() {
    final var acceptor1 = acceptorString();
    final var acceptor2 = acceptorString();

    // Test basic equality functionality - implementation details may vary
    assertNotNull(acceptor1._eq(acceptor2));
    assertNotNull(acceptor1._eq(acceptor1));

    // Test _eq with Any (polymorphic)
    assertNotNull(acceptor1._eq((Any) acceptor2));
    assertNotNull(acceptor1._eq((Any) acceptor1));

    // Test _eq with null and different type
    assertUnset.accept(acceptor1._eq(null));
    assertUnset.accept(acceptor1._eq(String._of("test")));
  }

  @Test
  void testStringAndHashOperators() {
    final var acceptor = acceptorString();

    // Test operator consistency - Functions should have consistent set/unset behavior
    assertNotNull(acceptor._isSet());
    assertNotNull(acceptor._string());
    assertNotNull(acceptor._hashcode());
  }

  @Test
  void testDelegationConsistency() {
    // Test delegation produces consistent behavior with base type
    final var acceptor = acceptorString();
    final var baseAcceptor = new Acceptor();

    // Both should be set and functional
    assertTrue.accept(acceptor._isSet());
    assertTrue.accept(baseAcceptor._isSet());
    assertNotNull(acceptor._string());
    assertNotNull(baseAcceptor._string());
  }

}
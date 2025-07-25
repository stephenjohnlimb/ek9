package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Test Acceptor of JSON parameterized function type.
 * This tests the delegation pattern and JSON-specific type safety.
 */
@SuppressWarnings({"checkstyle:MethodName", "checkstyle:AbbreviationAsWordInName", "checkstyle:TypeName"})
class _Acceptor_C11B98C5CC2836C6E2F7989D1C00504F1BCA1AC727BCB48944C8C88EA9306868Test extends Common {

  private static _Acceptor_C11B98C5CC2836C6E2F7989D1C00504F1BCA1AC727BCB48944C8C88EA9306868 acceptorJSON() {
    return _Acceptor_C11B98C5CC2836C6E2F7989D1C00504F1BCA1AC727BCB48944C8C88EA9306868._of();
  }

  private static _Acceptor_C11B98C5CC2836C6E2F7989D1C00504F1BCA1AC727BCB48944C8C88EA9306868 acceptorJSON(
      Acceptor value) {
    return _Acceptor_C11B98C5CC2836C6E2F7989D1C00504F1BCA1AC727BCB48944C8C88EA9306868._of(value);
  }


  @Test
  void testConstruction() {

    // Test factory method
    final var factoryAcceptor = acceptorJSON();
    assertNotNull(factoryAcceptor);
    assertSet.accept(factoryAcceptor);

    // Test factory method with base Acceptor
    final var baseAcceptor = new Acceptor();
    final var fromBase = acceptorJSON(baseAcceptor);
    assertNotNull(fromBase);
    assertSet.accept(fromBase);

    // Test factory method with null
    final var fromNull = acceptorJSON(null);
    assertNotNull(fromNull);
    assertSet.accept(fromNull);
  }

  @Test
  void testFunctionCall() {
    final var acceptor = acceptorJSON();
    assertNotNull(acceptor);

    // Test call with JSON - should not throw exception
    final var testJSON = JSON._of("{\"test\": \"value\"}");
    acceptor._call(testJSON);

    final var numberJSON = JSON._of("42");
    acceptor._call(numberJSON);

    // Test call with null JSON - should not throw exception
    acceptor._call(null);
  }

  @Test
  void testEquality() {
    final var acceptor1 = acceptorJSON();
    final var acceptor2 = acceptorJSON();

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
    final var acceptor = acceptorJSON();

    // Test operator consistency - Functions should have consistent set/unset behavior
    assertNotNull(acceptor._isSet());
    assertNotNull(acceptor._string());
    assertNotNull(acceptor._hashcode());
  }

  @Test
  void testDelegationConsistency() {
    // Test delegation produces consistent behavior with base type
    final var acceptor = acceptorJSON();
    final var baseAcceptor = new Acceptor();

    // Both should be set and functional
    assertTrue.accept(acceptor._isSet());
    assertTrue.accept(baseAcceptor._isSet());
    assertNotNull(acceptor._string());
    assertNotNull(baseAcceptor._string());
  }

}
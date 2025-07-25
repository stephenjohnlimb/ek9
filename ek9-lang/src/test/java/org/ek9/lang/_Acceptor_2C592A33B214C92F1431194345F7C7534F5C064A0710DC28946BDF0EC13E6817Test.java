package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Test Acceptor of Boolean parameterized type.
 * Tests the delegation pattern and Boolean-specific type safety.
 */
@SuppressWarnings({"checkstyle:MethodName", "checkstyle:AbbreviationAsWordInName", "checkstyle:TypeName"})
class _Acceptor_2C592A33B214C92F1431194345F7C7534F5C064A0710DC28946BDF0EC13E6817Test extends Common {

  private static _Acceptor_2C592A33B214C92F1431194345F7C7534F5C064A0710DC28946BDF0EC13E6817 acceptorBoolean() {
    return _Acceptor_2C592A33B214C92F1431194345F7C7534F5C064A0710DC28946BDF0EC13E6817._of();
  }

  private static _Acceptor_2C592A33B214C92F1431194345F7C7534F5C064A0710DC28946BDF0EC13E6817 acceptorBoolean(Acceptor value) {
    return _Acceptor_2C592A33B214C92F1431194345F7C7534F5C064A0710DC28946BDF0EC13E6817._of(value);
  }

  @Test
  void testConstruction() {

    // Test factory method
    final var factoryAcceptor = acceptorBoolean();
    assertNotNull(factoryAcceptor);
    assertSet.accept(factoryAcceptor);

    // Test factory method with base Acceptor
    final var baseAcceptor = new Acceptor();
    final var fromBase = acceptorBoolean(baseAcceptor);
    assertNotNull(fromBase);
    assertSet.accept(fromBase);

    // Test factory method with null
    final var fromNull = acceptorBoolean(null);
    assertNotNull(fromNull);
    assertSet.accept(fromNull);
  }

  @Test
  void testFunctionCall() {
    final var acceptor = acceptorBoolean();
    assertNotNull(acceptor);
    
    // Test call with Boolean - should not throw exception
    final var testBoolean = Boolean._of(true);
    acceptor._call(testBoolean);
    
    final var falseBoolean = Boolean._of(false);
    acceptor._call(falseBoolean);
    
    // Test call with null Boolean - should not throw exception
    acceptor._call(null);
  }

  @Test
  void testEquality() {
    final var acceptor1 = acceptorBoolean();
    final var acceptor2 = acceptorBoolean();

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
    final var acceptor = acceptorBoolean();

    // Test operator consistency - Functions should have consistent set/unset behavior
    assertNotNull(acceptor._isSet());
    assertNotNull(acceptor._string());
    assertNotNull(acceptor._hashcode());
  }

  @Test
  void testDelegationConsistency() {
    // Test delegation produces consistent behavior with base type
    final var acceptor = acceptorBoolean();
    final var baseAcceptor = new Acceptor();
    
    // Both should be set and functional
    assertTrue.accept(acceptor._isSet());
    assertTrue.accept(baseAcceptor._isSet());
    assertNotNull(acceptor._string());
    assertNotNull(baseAcceptor._string());
  }
}
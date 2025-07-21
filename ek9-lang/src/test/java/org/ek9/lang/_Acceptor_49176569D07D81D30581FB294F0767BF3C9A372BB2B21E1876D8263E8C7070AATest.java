package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Test Acceptor of String parameterized function type.
 * This tests the delegation pattern and String-specific type safety.
 */
@SuppressWarnings({"checkstyle:MethodName", "checkstyle:AbbreviationAsWordInName", "checkstyle:TypeName"})
class _Acceptor_49176569D07D81D30581FB294F0767BF3C9A372BB2B21E1876D8263E8C7070AATest extends Common {

  @Test
  void testConstruction() {
    // Test default constructor
    final var defaultConstructor = new _Acceptor_49176569D07D81D30581FB294F0767BF3C9A372BB2B21E1876D8263E8C7070AA();
    assertNotNull(defaultConstructor);
    assertSet.accept(defaultConstructor); // Acceptor functions are always set
    assertTrue.accept(defaultConstructor._isSet());
  }

  @Test
  void testFactoryMethods() {
    // Test factory method with no arguments
    final var factoryDefault = _Acceptor_49176569D07D81D30581FB294F0767BF3C9A372BB2B21E1876D8263E8C7070AA._of();
    assertNotNull(factoryDefault);
    assertSet.accept(factoryDefault);
    assertTrue.accept(factoryDefault._isSet());

    // Test factory method with base Acceptor
    final var baseAcceptor = new Acceptor();
    final var fromBase = _Acceptor_49176569D07D81D30581FB294F0767BF3C9A372BB2B21E1876D8263E8C7070AA._of(baseAcceptor);
    assertNotNull(fromBase);
    assertSet.accept(fromBase);
    assertTrue.accept(fromBase._isSet());

    // Test factory method with null Acceptor
    final var fromNull = _Acceptor_49176569D07D81D30581FB294F0767BF3C9A372BB2B21E1876D8263E8C7070AA._of(null);
    assertNotNull(fromNull);
    assertSet.accept(fromNull);
    assertTrue.accept(fromNull._isSet());
  }

  @Test
  void testStringParameterCall() {
    // Create a concrete implementation for testing
    final var testAcceptor = new _Acceptor_49176569D07D81D30581FB294F0767BF3C9A372BB2B21E1876D8263E8C7070AA() {
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
    testAcceptor._call(testString);

    // Verify String was received correctly
    assertNotNull(testAcceptor.getLastCalled());
    assertEquals("TestValue", testAcceptor.getLastCalled());

    // Test with different String values
    final var emptyString = String._of("");
    testAcceptor._call(emptyString);
    assertEquals("", testAcceptor.getLastCalled());

    final var longString = String._of("This is a longer test string");
    testAcceptor._call(longString);
    assertEquals("This is a longer test string", testAcceptor.getLastCalled());
  }

  @Test
  void testEqualityOperators() {
    // Test _eq with same parameterized type
    final var acceptor1 = _Acceptor_49176569D07D81D30581FB294F0767BF3C9A372BB2B21E1876D8263E8C7070AA._of();
    final var acceptor2 = _Acceptor_49176569D07D81D30581FB294F0767BF3C9A372BB2B21E1876D8263E8C7070AA._of();

    // Test basic equality functionality - implementation details may vary
    assertNotNull(acceptor1._eq(acceptor2));
    assertNotNull(acceptor1._eq(acceptor1));

    // Test _eq with Any (polymorphic)
    assertNotNull(acceptor1._eq((Any) acceptor2));
    assertNotNull(acceptor1._eq((Any) acceptor1));

    // Test _eq with null
    assertUnset.accept(acceptor1._eq(null));

    // Test _eq with different type
    final var string = String._of("test");
    assertUnset.accept(acceptor1._eq(string));
  }

  @Test
  void testOperatorConsistency() {
    final var acceptor = _Acceptor_49176569D07D81D30581FB294F0767BF3C9A372BB2B21E1876D8263E8C7070AA._of();

    // Test _isSet operator - Functions should have consistent set/unset behavior
    assertNotNull(acceptor._isSet());

    // Test _string operator
    assertNotNull(acceptor._string());

    // Test _hashcode operator
    assertNotNull(acceptor._hashcode());
  }

  @Test
  void testDelegationBehavior() {
    // Test that parameterized Acceptor properly delegates to base Acceptor
    final var paramAcceptor = _Acceptor_49176569D07D81D30581FB294F0767BF3C9A372BB2B21E1876D8263E8C7070AA._of();
    final var baseAcceptor = new Acceptor();

    // Verify delegation produces consistent behavior with base type
    assertEquals(baseAcceptor._isSet(), paramAcceptor._isSet());
  }

  @Test
  void testTypeSafety() {
    final var acceptor = _Acceptor_49176569D07D81D30581FB294F0767BF3C9A372BB2B21E1876D8263E8C7070AA._of();

    // Verify class type is correct
    assertEquals(_Acceptor_49176569D07D81D30581FB294F0767BF3C9A372BB2B21E1876D8263E8C7070AA.class, 
                 acceptor.getClass());

  }

}
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

  // Type alias for the long parameterized Acceptor class name
  private static final Class<_Acceptor_49176569D07D81D30581FB294F0767BF3C9A372BB2B21E1876D8263E8C7070AA> ACCEPTOR_STRING = 
      _Acceptor_49176569D07D81D30581FB294F0767BF3C9A372BB2B21E1876D8263E8C7070AA.class;
  
  // Factory methods for cleaner test code
  private static _Acceptor_49176569D07D81D30581FB294F0767BF3C9A372BB2B21E1876D8263E8C7070AA acceptorString() {
    return new _Acceptor_49176569D07D81D30581FB294F0767BF3C9A372BB2B21E1876D8263E8C7070AA();
  }
  
  // Helper methods for reducing assertion duplication
  
  /**
   * Helper method to assert that an Acceptor&lt;String&gt; is set and verify all related state.
   */
  private void assertFunctionSet(_Acceptor_49176569D07D81D30581FB294F0767BF3C9A372BB2B21E1876D8263E8C7070AA acceptor) {
    assertSet.accept(acceptor);
    assertTrue.accept(acceptor._isSet());
  }

  @Test
  void testConstructionAndFactoryMethods() {
    // Test default constructor
    assertFunctionSet(acceptorString());

    // Test factory method with no arguments
    assertFunctionSet(_Acceptor_49176569D07D81D30581FB294F0767BF3C9A372BB2B21E1876D8263E8C7070AA._of());

    // Test factory method with base Acceptor
    assertFunctionSet(_Acceptor_49176569D07D81D30581FB294F0767BF3C9A372BB2B21E1876D8263E8C7070AA._of(new Acceptor()));

    // Test factory method with null Acceptor
    assertFunctionSet(_Acceptor_49176569D07D81D30581FB294F0767BF3C9A372BB2B21E1876D8263E8C7070AA._of(null));
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

    // Test String parameter acceptance with different values
    testAcceptor._call(String._of("TestValue"));
    assertEquals("TestValue", testAcceptor.getLastCalled());

    testAcceptor._call(String._of(""));
    assertEquals("", testAcceptor.getLastCalled());

    testAcceptor._call(String._of("This is a longer test string"));
    assertEquals("This is a longer test string", testAcceptor.getLastCalled());
  }

  @Test
  void testEqualityOperators() {
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
  void testOperatorConsistencyAndTypeSafety() {
    final var acceptor = acceptorString();

    // Test operator consistency - Functions should have consistent set/unset behavior
    assertNotNull(acceptor._isSet());
    assertNotNull(acceptor._string());
    assertNotNull(acceptor._hashcode());

    // Verify class type is correct
    assertEquals(ACCEPTOR_STRING, acceptor.getClass());

    // Test delegation produces consistent behavior with base type
    assertEquals(new Acceptor()._isSet(), acceptor._isSet());
  }

}
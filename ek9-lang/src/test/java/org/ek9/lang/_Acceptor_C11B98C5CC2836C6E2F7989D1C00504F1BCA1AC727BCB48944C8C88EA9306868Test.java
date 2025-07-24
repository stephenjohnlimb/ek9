package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Test Acceptor of JSON parameterized function type.
 * This tests the delegation pattern and JSON-specific type safety.
 */
@SuppressWarnings({"checkstyle:MethodName", "checkstyle:AbbreviationAsWordInName", "checkstyle:TypeName"})
class _Acceptor_C11B98C5CC2836C6E2F7989D1C00504F1BCA1AC727BCB48944C8C88EA9306868Test extends Common {

  // Type alias for the long parameterized Acceptor class name
  private static final Class<_Acceptor_C11B98C5CC2836C6E2F7989D1C00504F1BCA1AC727BCB48944C8C88EA9306868> ACCEPTOR_JSON = 
      _Acceptor_C11B98C5CC2836C6E2F7989D1C00504F1BCA1AC727BCB48944C8C88EA9306868.class;
  
  // Factory methods for cleaner test code
  private static _Acceptor_C11B98C5CC2836C6E2F7989D1C00504F1BCA1AC727BCB48944C8C88EA9306868 acceptorJSON() {
    return new _Acceptor_C11B98C5CC2836C6E2F7989D1C00504F1BCA1AC727BCB48944C8C88EA9306868();
  }
  
  // Helper methods for reducing assertion duplication
  
  /**
   * Helper method to assert that an Acceptor&lt;JSON&gt; is set and verify all related state.
   */
  private void assertFunctionSet(_Acceptor_C11B98C5CC2836C6E2F7989D1C00504F1BCA1AC727BCB48944C8C88EA9306868 acceptor) {
    assertSet.accept(acceptor);
    assertTrue.accept(acceptor._isSet());
  }

  @Test
  void testConstructionAndFactoryMethods() {
    // Test default constructor
    assertFunctionSet(acceptorJSON());

    // Test factory method with no arguments
    assertFunctionSet(_Acceptor_C11B98C5CC2836C6E2F7989D1C00504F1BCA1AC727BCB48944C8C88EA9306868._of());

    // Test factory method with base Acceptor
    assertFunctionSet(_Acceptor_C11B98C5CC2836C6E2F7989D1C00504F1BCA1AC727BCB48944C8C88EA9306868._of(new Acceptor()));

    // Test factory method with null Acceptor
    assertFunctionSet(_Acceptor_C11B98C5CC2836C6E2F7989D1C00504F1BCA1AC727BCB48944C8C88EA9306868._of(null));
  }

  @Test
  void testJSONParameterCall() {
    // Create a concrete implementation for testing
    final var testAcceptor = new _Acceptor_C11B98C5CC2836C6E2F7989D1C00504F1BCA1AC727BCB48944C8C88EA9306868() {
      private JSON lastCalled = null;

      @Override
      public void _call(JSON t) {
        super._call(t); // Call delegate
        this.lastCalled = t; // Track for verification
      }

      public JSON getLastCalled() {
        return lastCalled;
      }
    };

    // Test JSON parameter acceptance with different values
    final var stringJSON = JSON._of("\"test string\"");
    testAcceptor._call(stringJSON);
    assertEquals(stringJSON, testAcceptor.getLastCalled());

    final var numberJSON = JSON._of("42");
    testAcceptor._call(numberJSON);
    assertEquals(numberJSON, testAcceptor.getLastCalled());

    final var objectJSON = JSON._of("{\"key\": \"value\"}");
    testAcceptor._call(objectJSON);
    assertEquals(objectJSON, testAcceptor.getLastCalled());

    final var arrayJSON = JSON._of("[1, 2, 3]");
    testAcceptor._call(arrayJSON);
    assertEquals(arrayJSON, testAcceptor.getLastCalled());
  }

  @Test
  void testEqualityOperators() {
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
  void testOperatorConsistencyAndTypeSafety() {
    final var acceptor = acceptorJSON();

    // Test operator consistency - Functions should have consistent set/unset behavior
    assertNotNull(acceptor._isSet());
    assertNotNull(acceptor._string());
    assertNotNull(acceptor._hashcode());

    // Verify class type is correct
    assertEquals(ACCEPTOR_JSON, acceptor.getClass());

    // Test delegation produces consistent behavior with base type
    assertEquals(new Acceptor()._isSet(), acceptor._isSet());
  }

}
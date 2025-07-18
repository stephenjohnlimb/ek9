package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class TCPConnectionTest extends Common {

  @Test
  void testAllMethodsResultInUnset() {
    final var theDefault = new TCPConnection() {
    };
    assertNotNull(theDefault);

    // Test that all methods return unset values by default
    assertUnset.accept(theDefault._isSet());
    assertUnset.accept(theDefault._string());
    
    // Test that methods don't throw exceptions
    assertDoesNotThrow(theDefault::output);
    assertDoesNotThrow(theDefault::input);
    
    // Test that returned objects are also unset
    assertUnset.accept(theDefault.output()._isSet());
    assertUnset.accept(theDefault.input()._isSet());
  }

  @Test
  void testOutputMethodReturnsStringOutput() {
    final var tcpConnection = new TCPConnection() {
    };
    assertNotNull(tcpConnection);

    final var output = tcpConnection.output();
    assertNotNull(output);
    
    // The returned StringOutput should be unset
    assertUnset.accept(output._isSet());
    
    // The StringOutput should have working methods (no-op default implementations)
    assertDoesNotThrow(() -> output.println(String._of("test")));
    assertDoesNotThrow(() -> output.print(String._of("test")));
    assertDoesNotThrow(() -> output._pipe(String._of("test")));
    assertDoesNotThrow(output::_close);
  }

  @Test
  void testInputMethodReturnsStringInput() {
    final var tcpConnection = new TCPConnection() {
    };
    assertNotNull(tcpConnection);

    final var input = tcpConnection.input();
    assertNotNull(input);
    
    // The returned StringInput should be unset
    assertUnset.accept(input._isSet());
    
    // The StringInput should have working methods (unset default implementations)
    assertUnset.accept(input.hasNext());
    assertUnset.accept(input.next());
    assertDoesNotThrow(input::_close);
  }

  @Test
  void testOperatorDefaults() {
    final var tcpConnection = new TCPConnection() {
    };
    assertNotNull(tcpConnection);

    // Test that operators return unset values by default
    assertUnset.accept(tcpConnection._isSet());
    assertUnset.accept(tcpConnection._string());
  }

  @Test
  void testTraitCanBeImplemented() {
    // Test that the trait can be implemented with custom behavior
    final var customTcpConnection = new TCPConnection() {
      @Override
      public Boolean _isSet() {
        return Boolean._of(true);
      }

      @Override
      public String _string() {
        return String._of("CustomTCPConnection");
      }
    };
    
    assertNotNull(customTcpConnection);
    
    // Test that custom implementations work
    assertSet.accept(customTcpConnection._isSet());
    assertSet.accept(customTcpConnection._string());
    assertEquals("CustomTCPConnection", customTcpConnection._string().state);
  }
}
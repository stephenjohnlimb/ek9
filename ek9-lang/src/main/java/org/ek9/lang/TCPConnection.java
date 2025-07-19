package org.ek9.lang;

import org.ek9tooling.Ek9Method;
import org.ek9tooling.Ek9Operator;
import org.ek9tooling.Ek9Trait;

/**
 * TCPConnection trait for EK9 - represents a TCP network connection interface.
 * <p>
 * This trait provides the basic interface for TCP connections, allowing
 * implementations to provide input and output streams for network communication.
 * </p>
 * <p>
 * Implementations should provide concrete behavior for establishing connections,
 * managing I/O operations, and handling connection lifecycle.
 * </p>
 */
@SuppressWarnings("checkstyle:MethodName")
@Ek9Trait("""
    TCPConnection as open""")
public interface TCPConnection extends Any {

  @Ek9Method("""
      output() as pure
        <- rtn as StringOutput?""")
  default StringOutput output() {
    // Default implementation returns unset StringOutput
    return new StringOutput() {
      @Override
      public Boolean _isSet() {
        return new Boolean();
      }
    };
  }

  @Ek9Method("""
      input() as pure
        <- rtn as StringInput?""")
  default StringInput input() {
    // Default implementation returns unset StringInput
    return new StringInput() {
      @Override
      public Boolean _isSet() {
        return new Boolean();
      }
    };
  }

  @Override
  @Ek9Operator("""
      operator ? as pure
        <- rtn as Boolean?""")
  default Boolean _isSet() {
    // Default implementation returns unset Boolean
    // Concrete implementations should define connection state
    return new Boolean();
  }

  @Override
  @Ek9Operator("""
      operator $ as pure
        <- rtn as String?""")
  default String _string() {
    // Default implementation returns unset String
    // Concrete implementations should provide connection details
    return new String();
  }

  @Ek9Operator("""
      operator close as pure""")
  default void _close() {
    //By default a 'no-op'.
  }

}
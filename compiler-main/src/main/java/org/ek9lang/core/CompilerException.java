package org.ek9lang.core;

/**
 * Our wrapper for an unchecked exception.
 */
public class CompilerException extends RuntimeException {

  public CompilerException(final String reason) {

    super(reason);

  }

  public CompilerException(final String reason, final Throwable cause) {

    super(reason, cause);

  }
}

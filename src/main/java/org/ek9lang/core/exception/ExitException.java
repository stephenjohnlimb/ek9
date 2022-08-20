package org.ek9lang.core.exception;

/**
 * An exception that when caught at the very outer edge of the compiler will cause the application
 * to exit with a specific error code. This is needed by the wider operating system and calling
 * programs/shell scripts.
 */
public class ExitException extends RuntimeException {
  private final int exitCode;

  public ExitException(int exitCode, String message) {
    super(message);
    this.exitCode = exitCode;
  }

  public ExitException(int exitCode, Throwable throwable) {
    super(throwable);
    this.exitCode = exitCode;
  }

  public int getExitCode() {
    return exitCode;
  }
}

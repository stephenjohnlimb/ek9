package org.ek9.lang;

/**
 * Interface for managing system exit behavior.
 * Allows for testable implementations that don't actually terminate the JVM.
 */
interface SystemExitManager {
  /**
   * Exit the system with the specified code.
   *
   * @param exitCode The exit code to use.
   */
  void exit(int exitCode);

  /**
   * Production implementation that calls System.exit().
   */
  class Production implements SystemExitManager {
    @Override
    public void exit(int exitCode) {
      System.exit(exitCode);
    }
  }

  /**
   * Test implementation that captures exit codes without terminating JVM.
   */
  class Test implements SystemExitManager {
    private java.lang.Integer lastExitCode = null;
    private boolean exitCalled = false;

    @Override
    public void exit(int exitCode) {
      this.lastExitCode = exitCode;
      this.exitCalled = true;
    }

    public java.lang.Integer getLastExitCode() {
      return lastExitCode;
    }

    public boolean wasExitCalled() {
      return exitCalled;
    }

    public void reset() {
      lastExitCode = null;
      exitCalled = false;
    }
  }
}
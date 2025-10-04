package org.ek9lang.integration;

/**
 * Container for process execution results.
 * Captures exit code, stdout, and stderr from executed processes.
 */
record ProcessResult(int exitCode, String stdout, String stderr) {

  /**
   * Check if stdout contains expected text.
   */
  public boolean stdoutContains(final String text) {
    return stdout != null && stdout.contains(text);
  }

}

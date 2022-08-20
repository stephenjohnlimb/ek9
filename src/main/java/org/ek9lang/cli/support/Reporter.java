package org.ek9lang.cli.support;

/**
 * Designed to ensure consistent reporting as part of the command line
 * commands and executions.
 */
public abstract class Reporter {
  private final boolean verbose;

  protected Reporter(boolean verbose) {
    this.verbose = verbose;
  }

  /**
   * Provide the report/log message prefix.
   */
  protected abstract String messagePrefix();

  protected void log(Object message) {
    if (verbose) {
      System.err.println(messagePrefix() + message);
    }
  }

  protected void report(Object message) {
    System.err.println(messagePrefix() + message);
  }
}

package org.ek9lang.cli;

import org.ek9lang.core.Logger;

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

  /**
   * Log a message to stderr if verbose enabled.
   */
  public void log(Object message) {
    if (verbose) {
      Logger.error(messagePrefix() + message);
    }
  }

  /**
   * Report a message to stderr.
   */
  public void report(Object message) {
    Logger.error(messagePrefix() + message);
  }
}

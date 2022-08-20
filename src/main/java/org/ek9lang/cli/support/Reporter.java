package org.ek9lang.cli.support;

import org.ek9lang.core.utils.Logger;

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
      Logger.error(messagePrefix() + message);
    }
  }

  protected void report(Object message) {
    Logger.error(messagePrefix() + message);
  }
}

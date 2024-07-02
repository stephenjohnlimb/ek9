package org.ek9lang.compiler.common;

import org.ek9lang.core.Logger;

/**
 * Designed to ensure consistent reporting as part of the command line
 * commands and executions.
 */
public abstract class Reporter {
  private final boolean verbose;
  private final boolean muteReportedErrors;

  /**
   * While it may seem strange to want to even be able to mute reported errors,
   * this class can/it used in developer tests and in some cases we don't want to see the actual errors.
   *
   * @param verbose            if true then compiler phase details and other general logging is output.
   * @param muteReportedErrors if true then even compiler errors are not reported (typically only useful in tests).
   */
  protected Reporter(final boolean verbose, final boolean muteReportedErrors) {

    this.verbose = verbose;
    this.muteReportedErrors = muteReportedErrors;

  }

  /**
   * Provide the report/log message prefix.
   */
  protected abstract String messagePrefix();

  /**
   * Log a message to stderr if verbose enabled.
   */
  public void log(final Object message) {

    if (verbose) {
      Logger.error(messagePrefix() + message);
    }

  }

  /**
   * Report a message to stderr.
   */
  public void report(final Object message) {

    if (!muteReportedErrors) {
      Logger.error(messagePrefix() + message);
    }

  }

  public boolean isVerbose() {

    return verbose;
  }

  public boolean isMuteReportedErrors() {

    return muteReportedErrors;
  }
}

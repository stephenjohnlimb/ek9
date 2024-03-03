package org.ek9lang.core;

/**
 * Wrapper for logging.
 */

//We disable the sonarlint warning as we want a simple logger for this compiler.
//This is a command line tool, and I don't want the bloated logging systems.
@SuppressWarnings("java:S106")
public class Logger {

  private static boolean debugEnabled = false;

  private static boolean muteStderrOutput = false;

  private Logger() {

  }

  /**
   * enable of disable debug output.
   *
   * @param enabled by default Logger disabled debug output.
   */
  public static void enableDebug(boolean enabled) {
    Logger.debugEnabled = enabled;
  }

  /**
   * Mute stderr output, but default the Logger uses stderr to report errors.
   *
   * @param muteStderrOutput by default Logger does not mute stderr, (for tests it is handy to switch off).
   */
  public static void setMuteStderrOutput(boolean muteStderrOutput) {
    Logger.muteStderrOutput = muteStderrOutput;
  }

  /**
   * Conditionally output debug information to stderr.
   */
  public static void debug(Object content) {
    if (debugEnabled && !muteStderrOutput) {
      System.err.println("DEBUG: " + content);
    }
  }

  /**
   * Logs output to stdout.
   */
  public static void log(String content) {
    if (!muteStderrOutput) {
      System.out.println(content);
    }
  }

  /**
   * Conditionally output debug information to stderr, using printf format.
   */
  public static void debugf(String format, Object... args) {
    if (debugEnabled && !muteStderrOutput) {
      System.err.printf(format, args);
    }
  }

  /**
   * Log an error unless muting is enabled.
   *
   * @param content The content to log to stderr.
   */
  public static void error(Object content) {
    if (!muteStderrOutput) {
      System.err.println(content);
    }
  }

  /**
   * Log a throwable stack trace unless muting is enabled.
   *
   * @param throwable The throwable and its stack to log to stderr.
   */
  public static void error(Throwable throwable) {
    if (!muteStderrOutput) {
      throwable.printStackTrace(System.err);
    }
  }
}

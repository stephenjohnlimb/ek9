package org.ek9lang.core;

/**
 * Wrapper for logging.
 */

//We disable the sonarlint warning as we want a simple logger for this compiler.
//This is a command line tool, and I don't want the bloated logging systems.
@SuppressWarnings("java:S106")
public class Logger {

  private static boolean debugEnabled = false;

  private Logger() {

  }

  public static void enableDebug(boolean enabled) {
    debugEnabled = enabled;
  }

  /**
   * Conditionally output debug information to stderr.
   */
  public static void debug(Object content) {
    if (debugEnabled) {
      System.err.println("DEBUG: " + content);
    }
  }

  /**
   * Logs output to stdout.
   */
  public static void log(String content) {
    System.out.println(content);
  }

  /**
   * Conditionally output debug information to stderr, using printf format.
   */
  public static void debugf(String format, Object... args) {
    if (debugEnabled) {
      System.err.printf(format, args);
    }
  }

  public static void error(Object content) {
    System.err.println(content);
  }

  public static void error(Throwable throwable) {
    System.err.println(throwable);
  }
}

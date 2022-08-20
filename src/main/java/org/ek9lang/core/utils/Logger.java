package org.ek9lang.core.utils;

/**
 * Wrapper for logging.
 */

//We disable the sonarlint warning as we want a simple logger for this compiler.
//This is a command line tool, and I don't want the bloated logging systems.
@SuppressWarnings("java:S106")
public class Logger {

  private Logger() {

  }

  public static void log(String content) {
    System.out.println(content);
  }

  public static void logf(String format, Object... args) {
    System.out.printf(format, args);
  }

  public static void error(Object content) {
    System.err.println(content);
  }

  public static void error(Throwable throwable) {
    System.err.println(throwable);
  }
}

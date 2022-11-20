package org.ek9lang.compiler.main;

/**
 * Conceptual compiler.
 */
public interface Compiler {

  /**
   * Compile what has been provided via some sort of workspace.
   * Expect that errors, warnings and the like are emitted through some type of
   * event. These can then be listened to and displayed.
   *
   * @return true if compilation succeeded
   */
  boolean compile();
}

package org.ek9lang.compiler.main;

import org.ek9lang.compiler.Workspace;

/**
 * Conceptual compiler.
 * The idea is that you create a 'workspace' with all the sources needed.
 * Obtain or setup whatever compiler flags you want.
 * Implement the CompilationListener, so you can get warning/error events
 * during compilation.
 * Then call 'compile' if you get true then compilation complete to the level you requested
 * (via flags) - you may get some warnings flow through your listener.
 * If you get false - then you will have got one or more errors (maybe some warnings).
 * Note that the workspace will need a list of all the files, as these still need parsing and
 * adding to the IR. But if their generated output already exists then the compiler can avoid
 * regenerating the same output if the source for that output has not been altered.
 */
public interface Compiler {

  /**
   * HERE FOR COMPILER ENTRY.
   * Compile what has been provided via some sort of workspace.
   * Expect that errors, warnings and the like are emitted through some type of
   * event. These can then be listened to and displayed.
   *
   * @return true if compilation succeeded
   */
  boolean compile(Workspace workspace, CompilerFlags flags);
}

package org.ek9lang.cli;

/**
 * Holds mutable compilation state that can be updated throughout
 * the execution chain. Allows nested E executions to report
 * compilation failures back to the top-level caller.
 */
final class CompilationResult {
  private boolean compilationFailed = false;

  void markCompilationFailed() {

    this.compilationFailed = true;

  }

  boolean hasCompilationFailed() {

    return compilationFailed;
  }

}

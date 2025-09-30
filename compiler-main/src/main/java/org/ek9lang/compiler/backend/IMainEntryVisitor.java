package org.ek9lang.compiler.backend;

/**
 * Interface for main entry point visitors that generate target-specific main entry points
 * for EK9 programs (e.g., main methods for JVM, main functions for native binaries).
 */
public interface IMainEntryVisitor {

  /**
   * Generate the main entry point for the target architecture.
   */
  void visit();

}
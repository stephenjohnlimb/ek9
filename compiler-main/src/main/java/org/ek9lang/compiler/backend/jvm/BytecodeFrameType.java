package org.ek9lang.compiler.backend.jvm;

/**
 * Control flow frame types for bytecode generation context stack.
 * Based on EK9 grammar (Ek9.g4) - actual control flow constructs only.
 */
public enum BytecodeFrameType {
  /**
   * Method/function/program scope.
   * Root frame for all bytecode generation.
   */
  METHOD,

  /**
   * Loop constructs: for-range, for-in, while, do-while.
   * <p>
   * Note: EK9 has no break/continue statements.
   * Loops execute to natural completion.
   * </p>
   */
  LOOP,

  /**
   * Switch/Given statement.
   * <p>
   * Note: Cases do NOT fall through in EK9.
   * Each case block automatically exits switch after execution.
   * </p>
   */
  SWITCH,

  /**
   * Try-catch-finally block.
   * Handles exception control flow and cleanup.
   */
  TRY_CATCH_FINALLY,

  /**
   * If/When statement chain.
   * Tracks end label for if-else-if-else chains.
   */
  IF_CHAIN
}

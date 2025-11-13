package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for try/finally statement (no catch).
 * This test validates finally-only execution without catch handler.
 * <p>
 * Tests:
 * - Try block execution
 * - Finally block execution on normal path
 * - Single catch-all exception table entry
 * - Finally duplication pattern (normal path + exception path)
 * - No explicit catch handler
 * </p>
 */
class TryFinallyTest extends AbstractBytecodeGenerationTest {

  public TryFinallyTest() {
    // Each bytecode test gets its own directory for parallel execution safety
    // Module name: bytecode.test, expected symbol count: 1 (the program)
    super("/examples/bytecodeGeneration/tryFinally",
        List.of(new SymbolCountCheck("bytecode.test", 1)),
        false, false, false);
  }

  /**
   * Disable debug instrumentation for minimal bytecode output.
   */
  @Override
  protected boolean addDebugInstrumentation() {
    return false;
  }
}

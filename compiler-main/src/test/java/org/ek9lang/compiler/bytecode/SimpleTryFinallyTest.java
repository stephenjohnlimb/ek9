package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for simple try/finally statement.
 * This test validates that finally block duplication is correctly generated
 * in JVM bytecode for try/finally blocks (no catch handlers).
 * <p>
 * Tests:
 * - Try block execution
 * - Finally block duplication (normal path + exception path)
 * - Exception table setup with catch-all handler
 * - Finally block always executes
 * - Exception rethrow after finally (exception path)
 * </p>
 */
class SimpleTryFinallyTest extends AbstractBytecodeGenerationTest {

  public SimpleTryFinallyTest() {
    // Each bytecode test gets its own directory for parallel execution safety
    // Module name: bytecode.test, expected symbol count: 1 (the program)
    super("/examples/bytecodeGeneration/simpleTryFinally",
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

package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for complete try/catch/finally statement.
 * This test validates that all three parts are correctly generated
 * in JVM bytecode with proper exception handling and finally duplication.
 * <p>
 * Tests:
 * - Try block execution
 * - Catch handler for specific exception type
 * - Finally block duplication (normal path + exception path)
 * - Two exception table entries (specific + catch-all)
 * - Finally executes after try/catch completion
 * - Finally executes before exception rethrow
 * </p>
 */
class TryCatchFinallyTest extends AbstractBytecodeGenerationTest {

  public TryCatchFinallyTest() {
    // Each bytecode test gets its own directory for parallel execution safety
    // Module name: bytecode.test, expected symbol count: 1 (the program)
    super("/examples/bytecodeGeneration/tryCatchFinally",
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

package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for simple try/catch statement.
 * This test validates that exception handling is correctly generated
 * in JVM bytecode for basic try/catch blocks where no exception is thrown.
 * <p>
 * Tests:
 * - Try block execution
 * - Exception table setup
 * - Normal flow (no exception thrown)
 * - Program flow continuation after try/catch
 * </p>
 */
class SimpleTryCatchTest extends AbstractBytecodeGenerationTest {

  public SimpleTryCatchTest() {
    // Each bytecode test gets its own directory for parallel execution safety
    // Module name: bytecode.test, expected symbol count: 1 (the program)
    super("/examples/bytecodeGeneration/simpleTryCatch",
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

package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for simple if statement (without else).
 * This test validates that conditional branching is correctly generated
 * in JVM bytecode for basic if statements.
 * <p>
 * Tests:
 * - If condition evaluation
 * - Conditional branching (ifeq/ifne)
 * - Body execution when condition is true
 * - Skipping body when condition is false
 * - Program flow continuation after if
 * </p>
 */
class SimpleIfStatementTest extends AbstractBytecodeGenerationTest {

  public SimpleIfStatementTest() {
    // Each bytecode test gets its own directory for parallel execution safety
    // Module name: bytecode.test, expected symbol count: 1 (the program)
    super("/examples/bytecodeGeneration/simpleIfStatement",
        List.of(new SymbolCountCheck("bytecode.test", 1)),
        false, false, false);
  }

  /**
   * Disable debug instrumentation for minimal bytecode output.
   * Debug info is already validated in HelloWorld and Boolean operator tests.
   */
  @Override
  protected boolean addDebugInstrumentation() {
    return false;
  }
}
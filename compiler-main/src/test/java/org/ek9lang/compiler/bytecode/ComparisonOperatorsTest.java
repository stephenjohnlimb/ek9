package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for Integer comparison operators.
 * Tests: ==, <>, <, <=, >, >=
 */
class ComparisonOperatorsTest extends AbstractBytecodeGenerationTest {

  public ComparisonOperatorsTest() {
    //Each bytecode test gets its own directory for parallel execution safety
    //Module name: bytecode.test, expected symbol count: 1 (the program)
    super("/examples/bytecodeGeneration/comparisonOperators",
        List.of(new SymbolCountCheck("bytecode.test", 1)),
        false, false, true);  // showBytecode = true initially
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

package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for Elvis coalescing operator (:?).
 * Tests: x ?: y returns x if x is not null AND set, otherwise returns y.
 */
class ElvisCoalescingOperatorTest extends AbstractBytecodeGenerationTest {

  public ElvisCoalescingOperatorTest() {
    //Each bytecode test gets its own directory for parallel execution safety
    //Module name: bytecode.test, expected symbol count: 1 (the program)
    super("/examples/bytecodeGeneration/elvisCoalescingOperator",
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

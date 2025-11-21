package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for less-than coalescing operator ({@code <? }).
 * Verifies correct handling of 5-case branching with null/unset checks on both operands.
 */
class LessThanCoalescingOperatorTest extends AbstractBytecodeGenerationTest {

  public LessThanCoalescingOperatorTest() {
    //Each bytecode test gets its own directory for parallel execution safety
    //Module name: bytecode.test, expected symbol count: 1 (the program)
    super("/examples/bytecodeGeneration/lessThanCoalescingOperator",
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

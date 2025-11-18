package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for multiple sequential null coalescing operators (??).
 * Verifies that each operator gets unique labels and proper bytecode structure.
 */
class MultipleNullCoalescingOperatorTest extends AbstractBytecodeGenerationTest {

  public MultipleNullCoalescingOperatorTest() {
    //Each bytecode test gets its own directory for parallel execution safety
    //Module name: bytecode.test, expected symbol count: 1 (the program)
    super("/examples/bytecodeGeneration/multipleNullCoalescingOperator",
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

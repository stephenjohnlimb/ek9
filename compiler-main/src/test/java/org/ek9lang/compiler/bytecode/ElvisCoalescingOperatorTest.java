package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for Elvis coalescing operator (:?).
 * Tests: x ?: y returns x if x is not null AND set, otherwise returns y.
 */
class ElvisCoalescingOperatorTest extends AbstractExecutableBytecodeTest {

  public ElvisCoalescingOperatorTest() {
    super("/examples/bytecodeGeneration/elvisCoalescingOperator",
        "bytecode.test",
        "ElvisCoalescingOperator",
        List.of(new SymbolCountCheck("bytecode.test", 1)));
  }
}

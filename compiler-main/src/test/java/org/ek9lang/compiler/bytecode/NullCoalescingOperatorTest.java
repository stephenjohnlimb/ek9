package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for null coalescing operator (??).
 * Tests: x ?? y returns x if x is not null, otherwise returns y.
 */
class NullCoalescingOperatorTest extends AbstractExecutableBytecodeTest {

  public NullCoalescingOperatorTest() {
    super("/examples/bytecodeGeneration/nullCoalescingOperator",
        "bytecode.test",
        "NullCoalescingOperator",
        List.of(new SymbolCountCheck("bytecode.test", 1)));
  }
}

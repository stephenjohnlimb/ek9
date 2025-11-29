package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for multiple sequential null coalescing operators (??).
 * Verifies that each operator gets unique labels and proper bytecode structure.
 */
class MultipleNullCoalescingOperatorTest extends AbstractExecutableBytecodeTest {

  public MultipleNullCoalescingOperatorTest() {
    super("/examples/bytecodeGeneration/multipleNullCoalescingOperator",
        "bytecode.test",
        "MultipleNullCoalescingOperator",
        List.of(new SymbolCountCheck("bytecode.test", 1)));
  }
}

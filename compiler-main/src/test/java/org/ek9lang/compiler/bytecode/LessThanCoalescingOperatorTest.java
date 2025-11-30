package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for less-than coalescing operator ({@code <? }).
 * Verifies correct handling of 5-case branching with null/unset checks on both operands.
 */
class LessThanCoalescingOperatorTest extends AbstractExecutableBytecodeTest {

  public LessThanCoalescingOperatorTest() {
    super("/examples/bytecodeGeneration/lessThanCoalescingOperator",
        "bytecode.test",
        "LessThanCoalescingOperator",
        List.of(new SymbolCountCheck("bytecode.test", 1)));
  }
}

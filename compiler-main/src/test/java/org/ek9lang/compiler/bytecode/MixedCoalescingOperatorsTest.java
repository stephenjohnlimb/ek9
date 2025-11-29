package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for mixed coalescing operators (?? and :?).
 * Verifies that different operator types maintain unique labels and correct bytecode.
 */
class MixedCoalescingOperatorsTest extends AbstractExecutableBytecodeTest {

  public MixedCoalescingOperatorsTest() {
    super("/examples/bytecodeGeneration/mixedCoalescingOperators",
        "bytecode.test",
        "MixedCoalescingOperators",
        List.of(new SymbolCountCheck("bytecode.test", 1)));
  }
}

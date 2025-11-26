package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation and execution for switch with multiple Character cases requiring promotion.
 */
class MultipleCaseCharacterPromotionTest extends AbstractExecutableBytecodeTest {
  public MultipleCaseCharacterPromotionTest() {
    super("/examples/bytecodeGeneration/multipleCaseCharacterPromotion",
        "bytecode.test.multichar",
        "MultipleCaseCharacterPromotion",
        List.of(new SymbolCountCheck("bytecode.test.multichar", 1)));
  }
}

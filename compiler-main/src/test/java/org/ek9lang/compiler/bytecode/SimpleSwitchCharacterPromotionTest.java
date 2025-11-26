package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation and execution for switch with Character-to-String promotion.
 */
class SimpleSwitchCharacterPromotionTest extends AbstractExecutableBytecodeTest {
  public SimpleSwitchCharacterPromotionTest() {
    super("/examples/bytecodeGeneration/simpleSwitchCharacterPromotion",
        "bytecode.test.character",
        "SimpleSwitchCharacterPromotion",
        List.of(new SymbolCountCheck("bytecode.test.character", 1)));
  }
}

package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for switch with Character-to-String promotion.
 */
class SimpleSwitchCharacterPromotionTest extends AbstractBytecodeGenerationTest {
  public SimpleSwitchCharacterPromotionTest() {
    super("/examples/bytecodeGeneration/simpleSwitchCharacterPromotion",
        List.of(new SymbolCountCheck("bytecode.test.character", 1)),
        false, false, false);
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;  // Clean bytecode for easier validation
  }
}

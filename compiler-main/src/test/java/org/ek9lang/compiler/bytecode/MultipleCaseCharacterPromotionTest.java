package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for switch with multiple Character cases requiring promotion.
 */
class MultipleCaseCharacterPromotionTest extends AbstractBytecodeGenerationTest {
  public MultipleCaseCharacterPromotionTest() {
    super("/examples/bytecodeGeneration/multipleCaseCharacterPromotion",
        List.of(new SymbolCountCheck("bytecode.test.multichar", 1)),
        false, false, false);
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;  // Clean bytecode for easier validation
  }
}

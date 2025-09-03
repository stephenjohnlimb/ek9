package org.ek9lang.compiler.ir;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Focused on testing IR generation for bitwise operator expressions.
 * Tests shift left (<<) and shift right (>>) operators with Bits type.
 */
class BitwiseOperatorTest extends AbstractIRGenerationTest {

  public BitwiseOperatorTest() {
    super("/examples/irGeneration/operatorUse/bitwise",
        List.of(
            new SymbolCountCheck(1, "shiftLeft.test", 1),
            new SymbolCountCheck(1, "shiftRight.test", 1)
        ), false, false, false);
  }

}
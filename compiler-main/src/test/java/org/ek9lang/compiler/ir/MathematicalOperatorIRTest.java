package org.ek9lang.compiler.ir;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Focussed in testing IR generation for mathematical operator expressions.
 */
class MathematicalOperatorIRTest extends AbstractIRGenerationTest {

  public MathematicalOperatorIRTest() {
    super("/examples/irGeneration/operatorUse/mathematical",
        List.of(
            new SymbolCountCheck(1, "power.test", 1),
            new SymbolCountCheck(1, "modulo.test", 1),
            new SymbolCountCheck(1, "remainder.test", 1),
            new SymbolCountCheck(1, "square_root.test", 1),
            new SymbolCountCheck(1, "absolute.test", 1),
            new SymbolCountCheck(1, "factorial.test", 1)
        ), false, false, false);
  }

}
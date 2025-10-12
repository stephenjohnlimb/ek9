package org.ek9lang.compiler.ir;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Focussed in testing IR generation for arithmetic expressions.
 */
class ArithmeticOperatorIRTest extends AbstractIRGenerationTest {

  public ArithmeticOperatorIRTest() {
    super("/examples/irGeneration/operatorUse/arithmetic",
        List.of(
            new SymbolCountCheck(1, "addition.test", 1),
            new SymbolCountCheck(1, "division.test", 1),
            new SymbolCountCheck(1, "multiplication.test", 1),
            new SymbolCountCheck(1, "negate.test", 1),
            new SymbolCountCheck(1, "subtraction.test", 1)
        ), false, false, false);
  }

}

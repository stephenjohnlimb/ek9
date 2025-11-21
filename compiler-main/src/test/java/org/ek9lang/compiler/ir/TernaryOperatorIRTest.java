package org.ek9lang.compiler.ir;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test IR generation for ternary operator (condition <- thenValue : elseValue).
 * Tests the expression form of if/else using EK9's unique ternary syntax.
 */
class TernaryOperatorIRTest extends AbstractIRGenerationTest {

  public TernaryOperatorIRTest() {
    super("/examples/irGeneration/operatorUse/ternary",
        List.of(
            new SymbolCountCheck(1, "simpleTernary.test", 1),
            new SymbolCountCheck(1, "ternaryQuestion.test", 1),
            new SymbolCountCheck(1, "ternaryInteger.test", 1)
        ), false, false, false);  // showIR = false (IR now validated via @IR directives)
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;
  }

}

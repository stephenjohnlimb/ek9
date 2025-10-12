package org.ek9lang.compiler.ir;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Focussed in testing IR generation for logical operator expressions.
 */
class LogicalOperatorIRTest extends AbstractIRGenerationTest {

  public LogicalOperatorIRTest() {
    super("/examples/irGeneration/operatorUse/logical",
        List.of(
            new SymbolCountCheck(1, "anand.test", 1),
            new SymbolCountCheck(1, "anor.test", 1),
            new SymbolCountCheck(1, "anot.test", 1),
            new SymbolCountCheck(1, "anxor.test", 1)
        ), false, false, false);
  }

}
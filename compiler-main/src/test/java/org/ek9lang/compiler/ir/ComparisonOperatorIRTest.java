package org.ek9lang.compiler.ir;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Focussed in testing IR generation for comparison expressions.
 */
class ComparisonOperatorIRTest extends AbstractIRGenerationTest {

  public ComparisonOperatorIRTest() {
    super("/examples/irGeneration/operatorUse/comparison",
        List.of(
            new SymbolCountCheck(1, "less_than.test", 1),
            new SymbolCountCheck(1, "less_equal.test", 1),
            new SymbolCountCheck(1, "greater_than.test", 1),
            new SymbolCountCheck(1, "greater_equal.test", 1),
            new SymbolCountCheck(1, "equals.test", 1),
            new SymbolCountCheck(1, "not_equals.test", 1),
            new SymbolCountCheck(1, "compare.test", 1)
        ), false, false, false);
  }

}
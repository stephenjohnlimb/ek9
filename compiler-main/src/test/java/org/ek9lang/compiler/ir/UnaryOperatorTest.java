package org.ek9lang.compiler.ir;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Focussed in testing IR generation for unary operator expressions.
 */
class UnaryOperatorTest extends AbstractIRGenerationTest {

  public UnaryOperatorTest() {
    super("/examples/irGeneration/operatorUse/unary",
        List.of(
            new SymbolCountCheck(1, "increment.test", 1),
            new SymbolCountCheck(1, "decrement.test", 1),
            new SymbolCountCheck(1, "isSet.test", 1),
            new SymbolCountCheck(1, "empty.test", 1)
        ), false, false, false);
  }

}
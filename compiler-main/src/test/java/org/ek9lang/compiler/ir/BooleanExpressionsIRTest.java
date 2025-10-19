package org.ek9lang.compiler.ir;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Focussed in testing IR generation for boolean expressions.
 */
class BooleanExpressionsIRTest extends AbstractIRGenerationTest {

  public BooleanExpressionsIRTest() {
    super("/examples/irGeneration/booleanExpressions",
        List.of(new SymbolCountCheck(3,"booleanExpressions", 3)
        ), false, false, true);
  }

}

package org.ek9lang.compiler.ir;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Focused on testing IR generation for conversion operator expressions.
 */
class ConversionOperatorTest extends AbstractIRGenerationTest {

  public ConversionOperatorTest() {
    super("/examples/irGeneration/operatorUse/conversion",
        List.of(
            new SymbolCountCheck(1, "string.test", 1),
            new SymbolCountCheck(1, "hashcode.test", 1),
            new SymbolCountCheck(1, "promote.test", 1),
            new SymbolCountCheck(1, "json.test", 1)
        ), false, false, false);
  }

}
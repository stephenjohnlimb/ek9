package org.ek9lang.compiler.ir;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Focused on testing IR generation for basic function definitions.
 * Tests simple functions, functions with multiple parameters,
 * and functions with return values.
 */
class FunctionsIRTest extends AbstractIRGenerationTest {

  public FunctionsIRTest() {
    super("/examples/irGeneration/functions",
        List.of(
            new SymbolCountCheck("ir.test.functions.simple", 1),
            new SymbolCountCheck("ir.test.functions.multiparams", 1),
            new SymbolCountCheck("ir.test.functions.returnvalue", 1)
        ), false, false, false);
  }

}

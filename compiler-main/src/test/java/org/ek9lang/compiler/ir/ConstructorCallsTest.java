package org.ek9lang.compiler.ir;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Focussed on testing IR generation for constructor calls, including explicit super() and this().
 */
class ConstructorCallsTest extends AbstractIRGenerationTest {

  public ConstructorCallsTest() {
    super("/examples/irGeneration/constructorCalls",
        List.of(new SymbolCountCheck(4, "constructorCalls", 6)
        ), false, false, false);
  }

}
package org.ek9lang.compiler.ir;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Focussed on testing IR generation for constructor calls, including explicit super() and this().
 */
class ConstructorCallsIRTest extends AbstractIRGenerationTest {

  public ConstructorCallsIRTest() {
    super("/examples/irGeneration/constructorCalls",
        List.of(new SymbolCountCheck(5, "constructorCalls", 8)
        ), false, false, false); // showIR=false - updated for syntheticConstructorExtendsRealClass.ek9
  }

}
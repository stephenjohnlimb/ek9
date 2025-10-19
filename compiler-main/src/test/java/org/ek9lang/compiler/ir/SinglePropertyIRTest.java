package org.ek9lang.compiler.ir;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Focussed in testing IR generation for a single property.
 */
class SinglePropertyIRTest extends AbstractIRGenerationTest {

  public SinglePropertyIRTest() {
    super("/examples/irGeneration/singleProperty",
        List.of(new SymbolCountCheck(1,"singleProperty", 1)
        ), false, false, false); // showIR=false
  }

}

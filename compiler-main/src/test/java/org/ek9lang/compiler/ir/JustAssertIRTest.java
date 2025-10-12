package org.ek9lang.compiler.ir;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Focussed in testing IR generation for simple classes.
 */
class JustAssertIRTest extends AbstractIRGenerationTest {

  public JustAssertIRTest() {
    super("/examples/irGeneration/justAssert",
        List.of(new SymbolCountCheck("justAssert", 1)
        ), false, false, false);
  }

}

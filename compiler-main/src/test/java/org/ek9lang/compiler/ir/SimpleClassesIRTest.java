package org.ek9lang.compiler.ir;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Focussed in testing IR generation for simple classes.
 */
class SimpleClassesIRTest extends AbstractIRGenerationTest {

  public SimpleClassesIRTest() {
    super("/examples/irGeneration/simpleClasses",
        List.of(new SymbolCountCheck("simpleClasses", 1)
        ), false, false, false);
  }

}

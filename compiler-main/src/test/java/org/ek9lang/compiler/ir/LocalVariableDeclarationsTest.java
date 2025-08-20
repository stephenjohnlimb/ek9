package org.ek9lang.compiler.ir;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Focussed in testing IR generation for simple classes.
 */
class LocalVariableDeclarationsTest extends AbstractIRGenerationTest {

  public LocalVariableDeclarationsTest() {
    super("/examples/irGeneration/localVariableDeclarations",
        List.of(new SymbolCountCheck(2,"local.variableDeclarations", 2)
        ), false, false);
  }

}

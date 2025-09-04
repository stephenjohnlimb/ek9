package org.ek9lang.compiler.ir;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Focused on testing IR generation of 'calls' methods, functions, etc.
 */
class CallsTest extends AbstractIRGenerationTest {

  public CallsTest() {
    super("/examples/irGeneration/calls",
        List.of(
            new SymbolCountCheck("functioncalls.test", 2)
        ), false, false, false);
  }

}
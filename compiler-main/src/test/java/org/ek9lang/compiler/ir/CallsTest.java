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
            new SymbolCountCheck("functioncalls.test", 2),
            new SymbolCountCheck("constructorcalls.test", 1),
            new SymbolCountCheck("constructorassignmentcalls.test", 1),
            new SymbolCountCheck("constructorcallswithargs.test", 1),
            new SymbolCountCheck("constructorassignmentwithargscalls.test", 1)
        ), false, false, false);
  }

}
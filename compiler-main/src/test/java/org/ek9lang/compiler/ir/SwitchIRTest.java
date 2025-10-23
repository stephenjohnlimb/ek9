package org.ek9lang.compiler.ir;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test IR generation for switch statement constructs.
 * Validates @IR directives in switch test files.
 */
class SwitchIRTest extends AbstractIRGenerationTest {

  public SwitchIRTest() {
    super("/examples/irGeneration/switches",
        List.of(new SymbolCountCheck(1, "controlFlow", 1)),  // 1 module, 1 function
        false, false, true);  // verbose=false, muteErrors=false, showIR=false
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;
  }
}

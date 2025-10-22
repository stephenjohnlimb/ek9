package org.ek9lang.compiler.ir;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test IR generation for loop constructs.
 */
class LoopIRTest extends AbstractIRGenerationTest {
  public LoopIRTest() {
    super("/examples/irGeneration/loops",
        List.of(new SymbolCountCheck(6, "loops", 6)),  // 6 modules (files), 6 functions total
        false, false, false);  // verbose=false, muteErrors=false, showIR=false
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;
  }
}

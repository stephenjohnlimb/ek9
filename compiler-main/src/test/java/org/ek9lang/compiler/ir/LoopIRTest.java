package org.ek9lang.compiler.ir;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test IR generation for loop constructs.
 */
class LoopIRTest extends AbstractIRGenerationTest {
  public LoopIRTest() {
    super("/examples/irGeneration/loops",
        List.of(new SymbolCountCheck(5, "loops", 5)),  // 5 modules (files), 5 functions total
        false, false, false);
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;
  }
}

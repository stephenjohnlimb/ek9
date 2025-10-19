package org.ek9lang.compiler.ir;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test IR generation for loop constructs.
 */
class LoopIRTest extends AbstractIRGenerationTest {
  public LoopIRTest() {
    super("/examples/irGeneration/loops",
        List.of(new SymbolCountCheck(2, "loops", 2)),  // 2 modules (files), 2 functions total
        false, false, false);
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;
  }
}

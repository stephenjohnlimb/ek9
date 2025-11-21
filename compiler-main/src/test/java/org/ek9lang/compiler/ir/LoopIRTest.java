package org.ek9lang.compiler.ir;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test IR generation for loop constructs.
 */
class LoopIRTest extends AbstractIRGenerationTest {
  public LoopIRTest() {
    super("/examples/irGeneration/loops",
        List.of(
            new SymbolCountCheck(6, "loops", 6),  // 6 modules without guards, 6 functions total
            new SymbolCountCheck(1, "loops.guard", 2),  // 1 module: simpleWhileWithGuard (2 functions: getValue + simpleWhileWithGuard)
            new SymbolCountCheck(1, "loops.doWhileGuard", 2)),  // 1 module: simpleDoWhileWithGuard (2 functions: getValue + simpleDoWhileWithGuard)
        false, false, false);  // verbose=false, muteErrors=false, showIR=false
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;
  }
}

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
            // While guard variants
            new SymbolCountCheck(1, "loops.guard", 2),  // simpleWhileWithGuard (2 functions: getValue + simpleWhileWithGuard)
            new SymbolCountCheck(1, "loops.assignGuard", 2),  // whileAssignmentGuard (2 functions)
            new SymbolCountCheck(1, "loops.guardedAssign", 2),  // whileGuardedAssignment (2 functions)
            new SymbolCountCheck(1, "loops.assignIfUnset", 2),  // whileAssignmentIfUnset (2 functions)
            // Do/While guard variants
            new SymbolCountCheck(1, "loops.doWhileGuard", 2),  // simpleDoWhileWithGuard (2 functions)
            new SymbolCountCheck(1, "loops.doAssignGuard", 2),  // doWhileAssignmentGuard (2 functions)
            new SymbolCountCheck(1, "loops.doGuardedAssign", 2),  // doWhileGuardedAssignment (2 functions)
            new SymbolCountCheck(1, "loops.doAssignIfUnset", 2),  // doWhileAssignmentIfUnset (2 functions)
            // For-range guard variants
            new SymbolCountCheck(1, "loops.forrange.guard", 2),  // forRangeWithGuard (2 functions)
            new SymbolCountCheck(1, "loops.forrange.assignGuard", 2),  // forRangeAssignmentGuard (2 functions)
            new SymbolCountCheck(1, "loops.forrange.guardedAssign", 2),  // forRangeGuardedAssignment (2 functions)
            new SymbolCountCheck(1, "loops.forrange.assignIfUnset", 2),  // forRangeAssignmentIfUnset (2 functions)
            // For-in guard variants
            new SymbolCountCheck(1, "loops.forin.guard", 2),  // forInWithGuard (2 functions)
            new SymbolCountCheck(1, "loops.forin.assignGuard", 2),  // forInAssignmentGuard (2 functions)
            new SymbolCountCheck(1, "loops.forin.guardedAssign", 2),  // forInGuardedAssignment (2 functions)
            new SymbolCountCheck(1, "loops.forin.assignIfUnset", 2)),  // forInAssignmentIfUnset (2 functions)
        false, false, false);  // verbose=false, muteErrors=false, showIR=true
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;
  }
}

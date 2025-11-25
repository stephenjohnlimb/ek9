package org.ek9lang.compiler.ir;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test IR generation for try statement guard variants.
 * Tests assignment guard (:=), guarded assignment (?=), and assignment if unset (:=?).
 */
class TryGuardVariantIRTest extends AbstractIRGenerationTest {
  public TryGuardVariantIRTest() {
    super("/examples/irGeneration/tryGuardVariants",
        List.of(
            // Try guard variants (new files without debug comments in @IR)
            new SymbolCountCheck(1, "tryGuardVariants.assignGuard", 2),  // tryAssignmentGuard (2 functions)
            new SymbolCountCheck(1, "tryGuardVariants.guardedAssign", 2),  // tryGuardedAssignment (2 functions)
            new SymbolCountCheck(1, "tryGuardVariants.assignIfUnset", 2)),  // tryAssignmentIfUnset (2 functions)
        false, false, false);  // verbose=false, muteErrors=false, showIR=true
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;
  }
}

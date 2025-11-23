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
        List.of(
            new SymbolCountCheck(1, "controlFlow", 1),  // simpleSwitchLiteral
            new SymbolCountCheck(1, "controlFlow.string", 1),  // simpleSwitchString
            new SymbolCountCheck(1, "controlFlow.float", 1),  // simpleSwitchFloat
            new SymbolCountCheck(1, "controlFlow.character", 1),  // simpleSwitchCharacterPromotion
            new SymbolCountCheck(1, "controlFlow.equality", 1),  // simpleSwitchExplicitEquality
            new SymbolCountCheck(1, "controlFlow.multiple", 1),  // multipleCaseLiterals
            new SymbolCountCheck(1, "controlFlow.multichar", 1),  // multipleCaseCharacterPromotion
            new SymbolCountCheck(1, "controlFlow.guard", 2),  // simpleSwitchWithGuard (2 functions: getValue, simpleSwitchWithGuard)
            new SymbolCountCheck(1, "controlFlow.guardControl", 2),  // switchWithGuardAndControl (2 functions: getOptionalValue, switchWithGuardAndControl)
            new SymbolCountCheck(1, "switches.assignGuard", 2),  // switchAssignmentGuard (2 functions: getValue, switchAssignmentGuard)
            new SymbolCountCheck(1, "switches.assignIfUnset", 2),  // switchAssignmentIfUnset (2 functions: getValue, switchAssignmentIfUnset)
            new SymbolCountCheck(1, "switches.guardedAssign", 2)),  // switchGuardedAssignment (2 functions: getValue, switchGuardedAssignment)
        false, false, true);  // verbose=false, muteErrors=false, showIR=true
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;
  }
}

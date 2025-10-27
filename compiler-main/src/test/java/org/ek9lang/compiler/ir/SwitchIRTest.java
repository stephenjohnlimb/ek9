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
            new SymbolCountCheck(1, "controlFlow.multichar", 1)),  // multipleCaseCharacterPromotion
        false, false, false);  // verbose=false, muteErrors=false, showIR=false
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;
  }
}

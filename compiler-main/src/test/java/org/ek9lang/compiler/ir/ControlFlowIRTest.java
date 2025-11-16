package org.ek9lang.compiler.ir;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Focused on testing IR generation for control flow statements.
 * Each test file contains a single function to keep IR output manageable.
 */
class ControlFlowIRTest extends AbstractIRGenerationTest {

  public ControlFlowIRTest() {
    super("/examples/irGeneration/controlFlow",
        List.of(new SymbolCountCheck(6, "controlFlow", 9)),
        false, false, false);
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;
  }
}
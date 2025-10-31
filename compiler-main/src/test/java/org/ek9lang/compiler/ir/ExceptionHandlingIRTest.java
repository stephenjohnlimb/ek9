package org.ek9lang.compiler.ir;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test IR generation for exception handling constructs (try/catch/finally).
 */
class ExceptionHandlingIRTest extends AbstractIRGenerationTest {
  public ExceptionHandlingIRTest() {
    super("/examples/irGeneration/exceptionHandling",
        List.of(new SymbolCountCheck(7, "exceptionHandling", 7)),  // 4 modules (files), 4 functions
        false, false, false);  // verbose=false, muteErrors=false, showIR=true (for analysis)
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return true;
  }
}

package org.ek9lang.compiler.ir;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test IR generation for exception handling constructs (try/catch/finally).
 */
class ExceptionHandlingIRTest extends AbstractIRGenerationTest {
  public ExceptionHandlingIRTest() {
    super("/examples/irGeneration/exceptionHandling",
        List.of(new SymbolCountCheck(10, "exceptionHandling", 13)),  // 10 modules, 13 symbols (7 funcs + 3 classes + 3 funcs)
        false, false, false);  // verbose=false, muteErrors=false, showIR=false
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return true;
  }
}

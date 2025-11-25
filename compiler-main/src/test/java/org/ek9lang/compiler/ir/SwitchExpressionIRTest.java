package org.ek9lang.compiler.ir;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Tests IR generation for switch expression form.
 * Expression form: result <- switch value <- rtn <- defaultValue
 */
class SwitchExpressionIRTest extends AbstractIRGenerationTest {

  public SwitchExpressionIRTest() {
    super("/examples/irGeneration/expressionForms",
        List.of(new SymbolCountCheck(1, "expressionForms", 1)
        ), false, false, false); // verbose=false, muteErrors=false, showIR=false
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;
  }

}

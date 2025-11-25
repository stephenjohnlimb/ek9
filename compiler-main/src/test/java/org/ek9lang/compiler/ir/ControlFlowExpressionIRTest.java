package org.ek9lang.compiler.ir;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Tests IR generation for control flow expression forms.
 * Expression forms: switch, while, do-while with accumulator pattern.
 * Example: result <- switch value <- rtn <- defaultValue
 * Example: result <- while condition <- rtn <- initialValue
 */
class ControlFlowExpressionIRTest extends AbstractIRGenerationTest {

  public ControlFlowExpressionIRTest() {
    super("/examples/irGeneration/expressionForms",
        List.of(
            new SymbolCountCheck(1, "expressionForms", 1),
            new SymbolCountCheck(1, "expressionForms.whileExpr", 1),
            new SymbolCountCheck(1, "expressionForms.doWhileExpr", 1)
        ), false, false, true); // verbose=false, muteErrors=false, showIR=true
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;
  }

}

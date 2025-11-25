package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for switch expression form.
 * Tests the expression form where switch returns a value via returningParam.
 * Pattern: result <- switch value <- rtn : defaultValue
 */
class SwitchExpressionTest extends AbstractBytecodeGenerationTest {

  public SwitchExpressionTest() {
    super("/examples/bytecodeGeneration/switchExpression",
        List.of(new SymbolCountCheck("bytecode.test", 1)),
        false, false, false);
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;
  }
}

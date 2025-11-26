package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for try expression form.
 * Tests the expression form where try returns a value via returningParam.
 * Pattern: result <- try <- rtn <- initialValue ... catch -> ex ... rtn: value
 *
 * <p>The test program computes input * 2 in try block:
 * <ul>
 *   <li>input=10: result = 20</li>
 *   <li>input=5: result = 10</li>
 *   <li>input=0: result = 0</li>
 * </ul>
 * </p>
 */
class TryExpressionTest extends AbstractBytecodeGenerationTest {

  public TryExpressionTest() {
    super("/examples/bytecodeGeneration/tryExpression",
        List.of(new SymbolCountCheck("bytecode.test.tryexpr", 1)),
        false, false, false);
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;
  }
}

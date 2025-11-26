package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation and in-JVM execution for switch expression form.
 * Tests the expression form where switch returns a value via returningParam.
 * Pattern: result <- switch value <- rtn : defaultValue
 *
 * <h2>Test Cases</h2>
 * <ul>
 *   <li>Case 1: value=1 -> "One"</li>
 *   <li>Case 2: value=2 -> "Two"</li>
 *   <li>Case 99: value=99 -> "Other" (default)</li>
 * </ul>
 */
class SwitchExpressionTest extends AbstractExecutableBytecodeTest {

  public SwitchExpressionTest() {
    super("/examples/bytecodeGeneration/switchExpression",
        "bytecode.test",
        "SwitchExpression",
        List.of(new SymbolCountCheck("bytecode.test", 1)));
  }
}

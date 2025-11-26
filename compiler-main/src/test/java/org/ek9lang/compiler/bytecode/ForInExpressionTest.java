package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation and execution for for-in expression form.
 * Tests the expression form where for-in returns a value via returningParam (accumulator pattern).
 * Pattern: result <- for item in collection <- rtn <- initialValue
 *
 * <p>The test program builds a list of integers [1..limit] and computes their sum:
 * <ul>
 *   <li>limit=5: list=[1,2,3,4,5], sum = 15</li>
 *   <li>limit=3: list=[1,2,3], sum = 6</li>
 *   <li>limit=0: list=[], sum = 0 (no iterations)</li>
 * </ul>
 * </p>
 */
class ForInExpressionTest extends AbstractExecutableBytecodeTest {
  public ForInExpressionTest() {
    super("/examples/bytecodeGeneration/forInExpression",
        "bytecode.test.forinexpr",
        "ForInExpression",
        List.of(new SymbolCountCheck("bytecode.test.forinexpr", 1)));
  }
}

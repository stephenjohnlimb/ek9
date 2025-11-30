package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for while expression form.
 * Tests the expression form where while returns a value via returningParam (accumulator pattern).
 * Pattern: result <- while guard <- value with condition <- rtn <- initialValue
 *
 * <p>The test program computes the sum of integers from 0 to limit-1:
 * <ul>
 *   <li>limit=5: sum = 0+1+2+3+4 = 10</li>
 *   <li>limit=3: sum = 0+1+2 = 3</li>
 *   <li>limit=0: sum = 0 (no iterations)</li>
 * </ul>
 * </p>
 */
class WhileExpressionTest extends AbstractExecutableBytecodeTest {

  public WhileExpressionTest() {
    super("/examples/bytecodeGeneration/whileExpression",
        "bytecode.test",
        "WhileExpression",
        List.of(new SymbolCountCheck("bytecode.test", 1)));
  }
}

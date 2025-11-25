package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for do-while expression form.
 * Tests the expression form where do-while returns a value via returningParam (accumulator pattern).
 * Pattern: result <- do guard <- value body while condition <- rtn <- initialValue
 *
 * <p>Unlike while, do-while executes body at least once before checking condition.
 * The test program computes the sum of integers from 0 to limit-1:
 * <ul>
 *   <li>limit=5: sum = 0+1+2+3+4 = 10</li>
 *   <li>limit=3: sum = 0+1+2 = 3</li>
 *   <li>limit=0: sum = 0 (body executes once with counter=0, then exits)</li>
 * </ul>
 * </p>
 */
class DoWhileExpressionTest extends AbstractBytecodeGenerationTest {

  public DoWhileExpressionTest() {
    super("/examples/bytecodeGeneration/doWhileExpression",
        List.of(new SymbolCountCheck("bytecode.test", 1)),
        false, false, false);
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;
  }
}

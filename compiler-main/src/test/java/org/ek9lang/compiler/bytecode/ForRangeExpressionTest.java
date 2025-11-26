package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation and execution for for-range expression form.
 * Tests the expression form where for-range returns a value via returningParam (accumulator pattern).
 * Pattern: result <- for i in start ... end <- rtn <- initialValue
 *
 * <p>The test program computes the sum of integers from 1 to limit:
 * <ul>
 *   <li>limit=5: sum = 1+2+3+4+5 = 15</li>
 *   <li>limit=3: sum = 1+2+3 = 6</li>
 *   <li>limit=0: sum = 0 (no iterations)</li>
 * </ul>
 * </p>
 */
class ForRangeExpressionTest extends AbstractExecutableBytecodeTest {
  public ForRangeExpressionTest() {
    super("/examples/bytecodeGeneration/forRangeExpression",
        "bytecode.test.forrangeexpr",
        "ForRangeExpression",
        List.of(new SymbolCountCheck("bytecode.test.forrangeexpr", 1)));
  }
}

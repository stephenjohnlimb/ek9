package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Minimal test to reproduce for-range loop bytecode bug.
 * Tests a for-range loop with a single nested if statement inside the loop body.
 *
 * <p>This is the SMALLEST example that should expose the jump target calculation bug
 * where nested control flow inside for-range loops causes incorrect bytecode generation.
 *
 * <p>Expected behavior:
 * <ul>
 *   <li>Should compile without errors</li>
 *   <li>Should generate valid bytecode that passes JVM verification</li>
 *   <li>Should execute correctly: print "Iteration" 5 times, "Big" 3 times, "Done" once</li>
 * </ul>
 *
 * <p>If this test FAILS with VerifyError, we've successfully isolated the minimal reproduction case.
 * If this test PASSES, the bug requires more complex nesting.
 */
class NestedIfInForRangeTest extends AbstractExecutableBytecodeTest {

  public NestedIfInForRangeTest() {
    super("/examples/bytecodeGeneration/nestedIfInForRange",
        "bytecode.test.nested",
        "NestedIfInForRange",
        List.of(new SymbolCountCheck("bytecode.test.nested", 1)));
  }
}

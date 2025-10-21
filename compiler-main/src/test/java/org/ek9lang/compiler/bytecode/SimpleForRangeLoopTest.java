package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for simple for-range loop.
 * Validates polymorphic dispatch and iteration control flow.
 * <p>
 * Tests:
 * </p>
 * <ul>
 *   <li>Initialization with ASSERT for set validation</li>
 *   <li>Direction detection (start <=> end)</li>
 *   <li>Three-way dispatch (ascending/descending/equal)</li>
 *   <li>Ascending case: Loop condition evaluation (current <= end)</li>
 *   <li>Body execution per iteration</li>
 *   <li>Increment operation (current++)</li>
 *   <li>Loop exit when condition false</li>
 *   <li>Final stdout output (runtime verification: should print 55)</li>
 * </ul>
 */
class SimpleForRangeLoopTest extends AbstractBytecodeGenerationTest {

  public SimpleForRangeLoopTest() {
    // One directory per test - parallel execution safety
    super("/examples/bytecodeGeneration/simpleForRangeLoop",
        List.of(new SymbolCountCheck("bytecode.test", 1)),
        false, false, false);
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;  // Clean bytecode for easier validation
  }
}

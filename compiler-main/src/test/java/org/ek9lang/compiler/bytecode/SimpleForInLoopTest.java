package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for simple for-in loop (iterator pattern).
 * Validates that iterator-based loops generate correct while-loop bytecode.
 * <p>
 * Tests:
 * </p>
 * <ul>
 *   <li>List.iterator() call to create Iterator</li>
 *   <li>While loop structure (condition + body)</li>
 *   <li>Condition: iterator.hasNext() evaluation</li>
 *   <li>Body: item = iterator.next() + user code</li>
 *   <li>Loop iteration and exit</li>
 *   <li>Final stdout output (runtime verification: should print "onetwothree")</li>
 * </ul>
 */
class SimpleForInLoopTest extends AbstractBytecodeGenerationTest {

  public SimpleForInLoopTest() {
    // One directory per test - parallel execution safety
    super("/examples/bytecodeGeneration/simpleForInLoop",
        List.of(new SymbolCountCheck("bytecode.test", 1)),
        false, false, false);  // showBytecode = false (final validation)
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;  // Clean bytecode for easier validation
  }
}

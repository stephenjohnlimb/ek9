package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for simple do-while loop.
 * Validates that loop control flow (body first, then condition)
 * is correctly generated in JVM bytecode.
 * <p>
 * Tests:
 * </p>
 * <ul>
 *   <li>Loop start label placement</li>
 *   <li>Body execution BEFORE condition</li>
 *   <li>Condition evaluation AFTER body</li>
 *   <li>Backward jump if TRUE (IFNE) instead of forward jump if FALSE</li>
 *   <li>Guaranteed at least one execution</li>
 *   <li>Stdout output (runtime validation: should print 5)</li>
 * </ul>
 */
class SimpleDoWhileLoopTest extends AbstractBytecodeGenerationTest {

  public SimpleDoWhileLoopTest() {
    // Each bytecode test gets its own directory for parallel execution safety
    super("/examples/bytecodeGeneration/simpleDoWhileLoop",
        List.of(new SymbolCountCheck("bytecode.test", 1)),
        false, false, false);  // Bytecode validation enabled
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;  // Minimal bytecode for easier validation
  }
}

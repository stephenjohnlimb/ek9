package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for try-with-resource statement (no catch).
 * This test validates implicit resource cleanup without explicit catch handler.
 * <p>
 * Tests:
 * - Try block with resource acquisition
 * - Implicit finally for resource cleanup (close() call)
 * - No explicit catch handler
 * - Exception table entries for cleanup
 * - Resource close() on normal and exception paths
 * </p>
 */
class TryWithResourceNoCatchTest extends AbstractBytecodeGenerationTest {

  public TryWithResourceNoCatchTest() {
    // Each bytecode test gets its own directory for parallel execution safety
    // Module name: bytecode.test, expected symbol count: 2 (program + TestResource class)
    super("/examples/bytecodeGeneration/tryWithResourceNoCatch",
        List.of(new SymbolCountCheck("bytecode.test", 2)),
        false, false, false);
  }

  /**
   * Disable debug instrumentation for minimal bytecode output.
   */
  @Override
  protected boolean addDebugInstrumentation() {
    return false;
  }
}

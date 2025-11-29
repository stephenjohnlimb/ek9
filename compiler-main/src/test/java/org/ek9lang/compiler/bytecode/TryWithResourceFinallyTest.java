package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for try-with-resource + finally statement (no catch).
 * This test validates both implicit resource cleanup AND explicit finally execution.
 * <p>
 * Tests:
 * - Try block with resource acquisition
 * - Implicit finally for resource cleanup (close() call)
 * - Explicit finally block execution
 * - Both implicit and explicit finally must execute
 * - Resource cleanup happens before explicit finally
 * - No explicit catch handler
 * - Exception table entries for both cleanup mechanisms
 * </p>
 */
class TryWithResourceFinallyTest extends AbstractExecutableBytecodeTest {

  public TryWithResourceFinallyTest() {
    super("/examples/bytecodeGeneration/tryWithResourceFinally",
        "bytecode.test",
        "TryWithResourceFinally",
        List.of(new SymbolCountCheck("bytecode.test", 2)));
  }
}

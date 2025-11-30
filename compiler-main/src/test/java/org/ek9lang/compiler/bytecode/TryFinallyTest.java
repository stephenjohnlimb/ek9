package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for try/finally statement (no catch).
 * This test validates finally-only execution without catch handler.
 * <p>
 * Tests:
 * - Try block execution
 * - Finally block execution on normal path
 * - Single catch-all exception table entry
 * - Finally duplication pattern (normal path + exception path)
 * - No explicit catch handler
 * </p>
 */
class TryFinallyTest extends AbstractExecutableBytecodeTest {

  public TryFinallyTest() {
    super("/examples/bytecodeGeneration/tryFinally",
        "bytecode.test",
        "TryFinally",
        List.of(new SymbolCountCheck("bytecode.test", 1)));
  }
}

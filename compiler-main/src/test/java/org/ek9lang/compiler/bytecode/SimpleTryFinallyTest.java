package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for simple try/finally statement.
 * This test validates that finally block duplication is correctly generated
 * in JVM bytecode for try/finally blocks (no catch handlers).
 * <p>
 * Tests:
 * - Try block execution
 * - Finally block duplication (normal path + exception path)
 * - Exception table setup with catch-all handler
 * - Finally block always executes
 * - Exception rethrow after finally (exception path)
 * </p>
 */
class SimpleTryFinallyTest extends AbstractExecutableBytecodeTest {

  public SimpleTryFinallyTest() {
    super("/examples/bytecodeGeneration/simpleTryFinally",
        "bytecode.test",
        "SimpleTryFinally",
        List.of(new SymbolCountCheck("bytecode.test", 1)));
  }
}

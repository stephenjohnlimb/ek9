package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for complete try/catch/finally statement.
 * This test validates that all three parts are correctly generated
 * in JVM bytecode with proper exception handling and finally duplication.
 * <p>
 * Tests:
 * - Try block execution
 * - Catch handler for specific exception type
 * - Finally block duplication (normal path + exception path)
 * - Two exception table entries (specific + catch-all)
 * - Finally executes after try/catch completion
 * - Finally executes before exception rethrow
 * </p>
 */
class TryCatchFinallyTest extends AbstractExecutableBytecodeTest {

  public TryCatchFinallyTest() {
    super("/examples/bytecodeGeneration/tryCatchFinally",
        "bytecode.test",
        "TryCatchFinally",
        List.of(new SymbolCountCheck("bytecode.test", 1)));
  }
}

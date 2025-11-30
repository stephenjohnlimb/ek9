package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for simple try/catch statement.
 * This test validates that exception handling is correctly generated
 * in JVM bytecode for basic try/catch blocks where no exception is thrown.
 * <p>
 * Tests:
 * - Try block execution
 * - Exception table setup
 * - Normal flow (no exception thrown)
 * - Program flow continuation after try/catch
 * </p>
 */
class SimpleTryCatchTest extends AbstractExecutableBytecodeTest {

  public SimpleTryCatchTest() {
    super("/examples/bytecodeGeneration/simpleTryCatch",
        "bytecode.test",
        "SimpleTryCatch",
        List.of(new SymbolCountCheck("bytecode.test", 1)));
  }
}

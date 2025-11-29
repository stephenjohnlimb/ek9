package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for try/catch statement (no finally).
 * This test validates simplest exception handling - just try and catch.
 * <p>
 * Tests:
 * - Try block execution
 * - Catch handler for specific exception type
 * - No finally block (no finally duplication)
 * - Single exception table entry (try → catch)
 * - No catch-all handler
 * - No duplicate handler
 * </p>
 */
class TryCatchTest extends AbstractExecutableBytecodeTest {

  public TryCatchTest() {
    super("/examples/bytecodeGeneration/tryCatch",
        "bytecode.test",
        "TryCatch",
        List.of(new SymbolCountCheck("bytecode.test", 1)));
  }
}

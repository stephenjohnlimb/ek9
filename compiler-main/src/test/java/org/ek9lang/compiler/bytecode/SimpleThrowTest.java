package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for simple throw statement.
 * Validates that THROW instruction generates correct ATHROW bytecode.
 */
class SimpleThrowTest extends AbstractBytecodeGenerationTest {

  public SimpleThrowTest() {
    // Module name: bytecode.test
    // Expected symbols: 1 program (TestSimpleThrow) = 1
    super("/examples/bytecodeGeneration/simpleThrow",
        List.of(new SymbolCountCheck("bytecode.test", 1)),
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

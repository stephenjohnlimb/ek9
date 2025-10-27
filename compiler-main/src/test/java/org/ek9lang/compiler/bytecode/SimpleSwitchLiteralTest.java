package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for simple switch with literal integer comparisons.
 */
class SimpleSwitchLiteralTest extends AbstractBytecodeGenerationTest {
  public SimpleSwitchLiteralTest() {
    super("/examples/bytecodeGeneration/simpleSwitchLiteral",
        List.of(new SymbolCountCheck("bytecode.test", 1)),
        false, false, false);
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;  // Clean bytecode for easier validation
  }
}

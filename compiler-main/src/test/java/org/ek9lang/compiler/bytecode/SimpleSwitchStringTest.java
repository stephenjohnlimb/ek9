package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for switch on String with contains/matches operators.
 */
class SimpleSwitchStringTest extends AbstractBytecodeGenerationTest {
  public SimpleSwitchStringTest() {
    super("/examples/bytecodeGeneration/simpleSwitchString",
        List.of(new SymbolCountCheck("bytecode.test.string", 1)),
        false, false, false);
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;  // Clean bytecode for easier validation
  }
}

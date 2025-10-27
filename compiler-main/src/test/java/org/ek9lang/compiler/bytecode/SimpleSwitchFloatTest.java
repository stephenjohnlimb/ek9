package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for switch on Float with comparison operators.
 */
class SimpleSwitchFloatTest extends AbstractBytecodeGenerationTest {
  public SimpleSwitchFloatTest() {
    super("/examples/bytecodeGeneration/simpleSwitchFloat",
        List.of(new SymbolCountCheck("bytecode.test.float", 1)),
        false, false, false);
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;  // Clean bytecode for easier validation
  }
}

package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for switch with Boolean cases.
 */
class SimpleSwitchBooleanTest extends AbstractBytecodeGenerationTest {
  public SimpleSwitchBooleanTest() {
    super("/examples/bytecodeGeneration/simpleSwitchBoolean",
        List.of(new SymbolCountCheck("bytecode.test.bool", 1)),
        false, false, false);
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;  // Clean bytecode for easier validation
  }
}

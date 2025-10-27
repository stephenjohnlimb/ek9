package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for switch with explicit equality operator.
 */
class SimpleSwitchExplicitEqualityTest extends AbstractBytecodeGenerationTest {
  public SimpleSwitchExplicitEqualityTest() {
    super("/examples/bytecodeGeneration/simpleSwitchExplicitEquality",
        List.of(new SymbolCountCheck("bytecode.test.equality", 1)),
        false, false, false);
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;  // Clean bytecode for easier validation
  }
}

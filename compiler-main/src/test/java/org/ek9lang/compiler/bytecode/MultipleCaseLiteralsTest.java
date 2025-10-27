package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for switch with multiple case expressions (OR logic).
 */
class MultipleCaseLiteralsTest extends AbstractBytecodeGenerationTest {
  public MultipleCaseLiteralsTest() {
    super("/examples/bytecodeGeneration/multipleCaseLiterals",
        List.of(new SymbolCountCheck("bytecode.test.multiple", 1)),
        false, false, false);
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;  // Clean bytecode for easier validation
  }
}

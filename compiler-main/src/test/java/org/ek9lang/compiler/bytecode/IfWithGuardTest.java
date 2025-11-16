package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for if statement with guard only (implicit isSet check).
 */
class IfWithGuardTest extends AbstractBytecodeGenerationTest {
  public IfWithGuardTest() {
    super("/examples/bytecodeGeneration/ifWithGuard",
        List.of(new SymbolCountCheck("bytecode.test", 1)),
        false, false, false);  // showBytecode=false - @BYTECODE directive in place
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;
  }
}

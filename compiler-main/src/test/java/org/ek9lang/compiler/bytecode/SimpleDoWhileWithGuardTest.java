package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for do-while statement with guard (implicit isSet check).
 */
class SimpleDoWhileWithGuardTest extends AbstractBytecodeGenerationTest {
  public SimpleDoWhileWithGuardTest() {
    super("/examples/bytecodeGeneration/simpleDoWhileWithGuard",
        List.of(new SymbolCountCheck("bytecode.test.dowhileguard", 1)),
        false, false, false);  // showBytecode=false - @BYTECODE directive in place
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;
  }
}

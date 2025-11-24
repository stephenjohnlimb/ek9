package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for for-in statement with guard (implicit isSet check).
 */
class ForInWithGuardTest extends AbstractBytecodeGenerationTest {
  public ForInWithGuardTest() {
    super("/examples/bytecodeGeneration/forInWithGuard",
        List.of(new SymbolCountCheck("bytecode.test.foringuard", 1)),
        false, false, false);  // showBytecode=false - @BYTECODE directive in place
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;
  }
}

package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for try statement with guard (implicit isSet check).
 */
class TryWithGuardTest extends AbstractBytecodeGenerationTest {
  public TryWithGuardTest() {
    super("/examples/bytecodeGeneration/tryWithGuard",
        List.of(new SymbolCountCheck("bytecode.test.tryguard", 1)),
        false, false, false);  // showBytecode=false - @BYTECODE directive in place
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;
  }
}

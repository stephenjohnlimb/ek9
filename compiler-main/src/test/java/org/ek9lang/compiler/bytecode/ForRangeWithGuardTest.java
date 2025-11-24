package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for for-range statement with guard (implicit isSet check).
 */
class ForRangeWithGuardTest extends AbstractBytecodeGenerationTest {
  public ForRangeWithGuardTest() {
    super("/examples/bytecodeGeneration/forRangeWithGuard",
        List.of(new SymbolCountCheck("bytecode.test.forrangeguard", 1)),
        false, false, false);  // showBytecode=false - @BYTECODE directive in place
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;
  }
}

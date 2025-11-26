package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation and execution for for-in statement with guard (implicit isSet check).
 */
class ForInWithGuardTest extends AbstractExecutableBytecodeTest {
  public ForInWithGuardTest() {
    super("/examples/bytecodeGeneration/forInWithGuard",
        "bytecode.test.foringuard",
        "ForInWithGuard",
        List.of(new SymbolCountCheck("bytecode.test.foringuard", 1)));
  }
}

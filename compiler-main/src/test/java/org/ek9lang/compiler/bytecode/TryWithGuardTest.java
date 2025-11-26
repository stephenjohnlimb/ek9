package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation and execution for try statement with guard (implicit isSet check).
 */
class TryWithGuardTest extends AbstractExecutableBytecodeTest {
  public TryWithGuardTest() {
    super("/examples/bytecodeGeneration/tryWithGuard",
        "bytecode.test.tryguard",
        "TryWithGuard",
        List.of(new SymbolCountCheck("bytecode.test.tryguard", 1)));
  }
}

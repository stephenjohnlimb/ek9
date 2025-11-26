package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation and execution for for-range statement with guard (implicit isSet check).
 */
class ForRangeWithGuardTest extends AbstractExecutableBytecodeTest {
  public ForRangeWithGuardTest() {
    super("/examples/bytecodeGeneration/forRangeWithGuard",
        "bytecode.test.forrangeguard",
        "ForRangeWithGuard",
        List.of(new SymbolCountCheck("bytecode.test.forrangeguard", 1)));
  }
}

package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation and execution for do-while statement with guard (implicit isSet check).
 */
class SimpleDoWhileWithGuardTest extends AbstractExecutableBytecodeTest {
  public SimpleDoWhileWithGuardTest() {
    super("/examples/bytecodeGeneration/simpleDoWhileWithGuard",
        "bytecode.test.dowhileguard",
        "SimpleDoWhileWithGuard",
        List.of(new SymbolCountCheck("bytecode.test.dowhileguard", 1)));
  }
}

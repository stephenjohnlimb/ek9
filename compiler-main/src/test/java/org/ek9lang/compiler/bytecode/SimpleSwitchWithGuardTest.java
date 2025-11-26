package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation and execution for switch statement with guard (implicit isSet check).
 */
class SimpleSwitchWithGuardTest extends AbstractExecutableBytecodeTest {
  public SimpleSwitchWithGuardTest() {
    super("/examples/bytecodeGeneration/simpleSwitchWithGuard",
        "bytecode.test.switchguard",
        "SimpleSwitchWithGuard",
        List.of(new SymbolCountCheck("bytecode.test.switchguard", 1)));
  }
}

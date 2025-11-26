package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation and execution for if statement with guard only (implicit isSet check).
 */
class IfWithGuardTest extends AbstractExecutableBytecodeTest {
  public IfWithGuardTest() {
    super("/examples/bytecodeGeneration/ifWithGuard",
        "bytecode.test",
        "IfWithGuard",
        List.of(new SymbolCountCheck("bytecode.test", 1)));
  }
}

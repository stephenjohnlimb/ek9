package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation and execution for switch with explicit equality operator.
 */
class SimpleSwitchExplicitEqualityTest extends AbstractExecutableBytecodeTest {
  public SimpleSwitchExplicitEqualityTest() {
    super("/examples/bytecodeGeneration/simpleSwitchExplicitEquality",
        "bytecode.test.equality",
        "SimpleSwitchExplicitEquality",
        List.of(new SymbolCountCheck("bytecode.test.equality", 1)));
  }
}

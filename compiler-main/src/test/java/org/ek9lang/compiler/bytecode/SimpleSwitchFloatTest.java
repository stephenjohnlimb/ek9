package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation and execution for switch on Float with comparison operators.
 */
class SimpleSwitchFloatTest extends AbstractExecutableBytecodeTest {
  public SimpleSwitchFloatTest() {
    super("/examples/bytecodeGeneration/simpleSwitchFloat",
        "bytecode.test.float",
        "SimpleSwitchFloat",
        List.of(new SymbolCountCheck("bytecode.test.float", 1)));
  }
}

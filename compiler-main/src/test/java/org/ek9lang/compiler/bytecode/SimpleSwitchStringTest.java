package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation and execution for switch on String with contains/matches operators.
 */
class SimpleSwitchStringTest extends AbstractExecutableBytecodeTest {
  public SimpleSwitchStringTest() {
    super("/examples/bytecodeGeneration/simpleSwitchString",
        "bytecode.test.string",
        "SimpleSwitchString",
        List.of(new SymbolCountCheck("bytecode.test.string", 1)));
  }
}

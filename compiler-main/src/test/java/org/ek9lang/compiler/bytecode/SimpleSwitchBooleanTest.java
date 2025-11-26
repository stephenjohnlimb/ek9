package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation and execution for switch with Boolean cases.
 */
class SimpleSwitchBooleanTest extends AbstractExecutableBytecodeTest {
  public SimpleSwitchBooleanTest() {
    super("/examples/bytecodeGeneration/simpleSwitchBoolean",
        "bytecode.test.bool",
        "SimpleSwitchBoolean",
        List.of(new SymbolCountCheck("bytecode.test.bool", 1)));
  }
}

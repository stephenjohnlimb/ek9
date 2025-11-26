package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation and execution for switch with guard and control flow.
 */
class SwitchWithGuardAndControlTest extends AbstractExecutableBytecodeTest {
  public SwitchWithGuardAndControlTest() {
    super("/examples/bytecodeGeneration/switchWithGuardAndControl",
        "bytecode.test.switchguardcontrol",
        "SwitchWithGuardAndControl",
        List.of(new SymbolCountCheck("bytecode.test.switchguardcontrol", 1)));
  }
}

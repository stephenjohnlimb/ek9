package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation and execution for switch with guarded assignment (?=).
 */
class SwitchGuardedAssignmentTest extends AbstractExecutableBytecodeTest {
  public SwitchGuardedAssignmentTest() {
    super("/examples/bytecodeGeneration/switchGuardedAssignment",
        "bytecode.test.switchguardedassign",
        "SwitchGuardedAssignment",
        List.of(new SymbolCountCheck("bytecode.test.switchguardedassign", 1)));
  }
}

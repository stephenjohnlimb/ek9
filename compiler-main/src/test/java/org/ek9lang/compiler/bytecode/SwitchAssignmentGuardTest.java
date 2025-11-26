package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation and execution for switch with assignment guard (:=).
 */
class SwitchAssignmentGuardTest extends AbstractExecutableBytecodeTest {
  public SwitchAssignmentGuardTest() {
    super("/examples/bytecodeGeneration/switchAssignmentGuard",
        "bytecode.test.switchassignguard",
        "SwitchAssignmentGuard",
        List.of(new SymbolCountCheck("bytecode.test.switchassignguard", 1)));
  }
}

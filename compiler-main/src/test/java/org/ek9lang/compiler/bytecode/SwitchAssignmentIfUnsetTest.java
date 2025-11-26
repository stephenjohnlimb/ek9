package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation and execution for switch with assignment if unset (:=?).
 */
class SwitchAssignmentIfUnsetTest extends AbstractExecutableBytecodeTest {
  public SwitchAssignmentIfUnsetTest() {
    super("/examples/bytecodeGeneration/switchAssignmentIfUnset",
        "bytecode.test.switchassignifunset",
        "SwitchAssignmentIfUnset",
        List.of(new SymbolCountCheck("bytecode.test.switchassignifunset", 1)));
  }
}

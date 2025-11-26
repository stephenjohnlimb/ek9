package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation and execution for if statement with assignment if unset (:=?) without condition.
 */
class IfAssignmentIfUnsetTest extends AbstractExecutableBytecodeTest {
  public IfAssignmentIfUnsetTest() {
    super("/examples/bytecodeGeneration/ifAssignmentIfUnset",
        "bytecode.test",
        "IfAssignmentIfUnset",
        List.of(new SymbolCountCheck("bytecode.test", 1)));
  }
}

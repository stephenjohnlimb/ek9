package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation and execution for if statement with assignment if unset (:=?) with condition.
 */
class IfAssignmentIfUnsetAndConditionTest extends AbstractExecutableBytecodeTest {
  public IfAssignmentIfUnsetAndConditionTest() {
    super("/examples/bytecodeGeneration/ifAssignmentIfUnsetAndCondition",
        "bytecode.test",
        "IfAssignmentIfUnsetAndCondition",
        List.of(new SymbolCountCheck("bytecode.test", 1)));
  }
}

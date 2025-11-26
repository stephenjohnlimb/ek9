package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation and execution for if statement with guarded assignment (?=) with condition.
 */
class IfGuardedAssignmentAndConditionTest extends AbstractExecutableBytecodeTest {
  public IfGuardedAssignmentAndConditionTest() {
    super("/examples/bytecodeGeneration/ifGuardedAssignmentAndCondition",
        "bytecode.test",
        "IfGuardedAssignmentAndCondition",
        List.of(new SymbolCountCheck("bytecode.test", 1)));
  }
}

package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation and execution for if statement with assignment guard (:=) with condition.
 */
class IfAssignmentGuardAndConditionTest extends AbstractExecutableBytecodeTest {
  public IfAssignmentGuardAndConditionTest() {
    super("/examples/bytecodeGeneration/ifAssignmentGuardAndCondition",
        "bytecode.test",
        "IfAssignmentGuardAndCondition",
        List.of(new SymbolCountCheck("bytecode.test", 1)));
  }
}

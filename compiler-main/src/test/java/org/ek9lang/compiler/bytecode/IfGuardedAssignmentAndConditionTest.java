package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for if statement with guarded assignment (?=) with condition.
 */
class IfGuardedAssignmentAndConditionTest extends AbstractBytecodeGenerationTest {
  public IfGuardedAssignmentAndConditionTest() {
    super("/examples/bytecodeGeneration/ifGuardedAssignmentAndCondition",
        List.of(new SymbolCountCheck("bytecode.test", 1)),
        false, false, false);
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;
  }
}

package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for if statement with assignment if unset (:=?) with condition.
 */
class IfAssignmentIfUnsetAndConditionTest extends AbstractBytecodeGenerationTest {
  public IfAssignmentIfUnsetAndConditionTest() {
    super("/examples/bytecodeGeneration/ifAssignmentIfUnsetAndCondition",
        List.of(new SymbolCountCheck("bytecode.test", 1)),
        false, false, false);
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;
  }
}

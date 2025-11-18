package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for if statement with assignment guard (:=) with condition.
 */
class IfAssignmentGuardAndConditionTest extends AbstractBytecodeGenerationTest {
  public IfAssignmentGuardAndConditionTest() {
    super("/examples/bytecodeGeneration/ifAssignmentGuardAndCondition",
        List.of(new SymbolCountCheck("bytecode.test", 1)),
        false, false, false);  // @BYTECODE directive now in place
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;
  }
}

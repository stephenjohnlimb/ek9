package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for if statement with guarded assignment (?=) without condition.
 */
class IfGuardedAssignmentTest extends AbstractBytecodeGenerationTest {
  public IfGuardedAssignmentTest() {
    super("/examples/bytecodeGeneration/ifGuardedAssignment",
        List.of(new SymbolCountCheck("bytecode.test", 1)),
        false, false, false);
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;
  }
}

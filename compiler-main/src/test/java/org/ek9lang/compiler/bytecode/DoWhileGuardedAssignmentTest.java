package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

class DoWhileGuardedAssignmentTest extends AbstractBytecodeGenerationTest {
  public DoWhileGuardedAssignmentTest() {
    super("/examples/bytecodeGeneration/doWhileGuardedAssignment",
        List.of(new SymbolCountCheck("bytecode.test.dowhileguardedassign", 1)),
        false, false, false);
  }
  @Override
  protected boolean addDebugInstrumentation() { return false; }
}

package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

class DoWhileAssignmentGuardTest extends AbstractBytecodeGenerationTest {
  public DoWhileAssignmentGuardTest() {
    super("/examples/bytecodeGeneration/doWhileAssignmentGuard",
        List.of(new SymbolCountCheck("bytecode.test.dowhileassignguard", 1)),
        false, false, false);
  }
  @Override
  protected boolean addDebugInstrumentation() { return false; }
}

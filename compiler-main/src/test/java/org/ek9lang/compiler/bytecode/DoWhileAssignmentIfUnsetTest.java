package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

class DoWhileAssignmentIfUnsetTest extends AbstractBytecodeGenerationTest {
  public DoWhileAssignmentIfUnsetTest() {
    super("/examples/bytecodeGeneration/doWhileAssignmentIfUnset",
        List.of(new SymbolCountCheck("bytecode.test.dowhileassignifunset", 1)),
        false, false, false);
  }
  @Override
  protected boolean addDebugInstrumentation() { return false; }
}

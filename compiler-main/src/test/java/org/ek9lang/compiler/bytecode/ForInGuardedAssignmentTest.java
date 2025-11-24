package org.ek9lang.compiler.bytecode;
import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

class ForInGuardedAssignmentTest extends AbstractBytecodeGenerationTest {
  public ForInGuardedAssignmentTest() {
    super("/examples/bytecodeGeneration/forInGuardedAssignment",
        List.of(new SymbolCountCheck("bytecode.test.foringuardedassign", 1)),
        false, false, false);
  }
  @Override protected boolean addDebugInstrumentation() { return false; }
}

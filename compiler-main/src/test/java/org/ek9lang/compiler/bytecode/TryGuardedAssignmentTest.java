package org.ek9lang.compiler.bytecode;
import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

class TryGuardedAssignmentTest extends AbstractBytecodeGenerationTest {
  public TryGuardedAssignmentTest() {
    super("/examples/bytecodeGeneration/tryGuardedAssignment",
        List.of(new SymbolCountCheck("bytecode.test.tryguardedassign", 1)),
        false, false, false);
  }
  @Override protected boolean addDebugInstrumentation() { return false; }
}

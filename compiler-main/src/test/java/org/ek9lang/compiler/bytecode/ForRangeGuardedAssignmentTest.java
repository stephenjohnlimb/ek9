package org.ek9lang.compiler.bytecode;
import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

class ForRangeGuardedAssignmentTest extends AbstractBytecodeGenerationTest {
  public ForRangeGuardedAssignmentTest() {
    super("/examples/bytecodeGeneration/forRangeGuardedAssignment",
        List.of(new SymbolCountCheck("bytecode.test.forrangeguardedassign", 1)),
        false, false, false);
  }
  @Override protected boolean addDebugInstrumentation() { return false; }
}

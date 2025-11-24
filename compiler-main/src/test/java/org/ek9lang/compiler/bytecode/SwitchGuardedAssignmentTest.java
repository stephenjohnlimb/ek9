package org.ek9lang.compiler.bytecode;
import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

class SwitchGuardedAssignmentTest extends AbstractBytecodeGenerationTest {
  public SwitchGuardedAssignmentTest() {
    super("/examples/bytecodeGeneration/switchGuardedAssignment",
        List.of(new SymbolCountCheck("bytecode.test.switchguardedassign", 1)),
        false, false, false);
  }
  @Override protected boolean addDebugInstrumentation() { return false; }
}

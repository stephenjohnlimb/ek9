package org.ek9lang.compiler.bytecode;
import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

class SwitchAssignmentGuardTest extends AbstractBytecodeGenerationTest {
  public SwitchAssignmentGuardTest() {
    super("/examples/bytecodeGeneration/switchAssignmentGuard",
        List.of(new SymbolCountCheck("bytecode.test.switchassignguard", 1)),
        false, false, false);
  }
  @Override protected boolean addDebugInstrumentation() { return false; }
}

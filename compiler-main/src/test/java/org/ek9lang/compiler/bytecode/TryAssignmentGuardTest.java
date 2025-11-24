package org.ek9lang.compiler.bytecode;
import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

class TryAssignmentGuardTest extends AbstractBytecodeGenerationTest {
  public TryAssignmentGuardTest() {
    super("/examples/bytecodeGeneration/tryAssignmentGuard",
        List.of(new SymbolCountCheck("bytecode.test.tryassignguard", 1)),
        false, false, false);
  }
  @Override protected boolean addDebugInstrumentation() { return false; }
}

package org.ek9lang.compiler.bytecode;
import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

class ForInAssignmentGuardTest extends AbstractBytecodeGenerationTest {
  public ForInAssignmentGuardTest() {
    super("/examples/bytecodeGeneration/forInAssignmentGuard",
        List.of(new SymbolCountCheck("bytecode.test.forinassignguard", 1)),
        false, false, false);
  }
  @Override protected boolean addDebugInstrumentation() { return false; }
}

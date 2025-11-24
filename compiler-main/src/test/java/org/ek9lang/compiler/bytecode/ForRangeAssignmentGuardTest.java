package org.ek9lang.compiler.bytecode;
import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

class ForRangeAssignmentGuardTest extends AbstractBytecodeGenerationTest {
  public ForRangeAssignmentGuardTest() {
    super("/examples/bytecodeGeneration/forRangeAssignmentGuard",
        List.of(new SymbolCountCheck("bytecode.test.forrangeassignguard", 1)),
        false, false, false);
  }
  @Override protected boolean addDebugInstrumentation() { return false; }
}

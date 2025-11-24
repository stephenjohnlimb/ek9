package org.ek9lang.compiler.bytecode;
import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

class TryAssignmentIfUnsetTest extends AbstractBytecodeGenerationTest {
  public TryAssignmentIfUnsetTest() {
    super("/examples/bytecodeGeneration/tryAssignmentIfUnset",
        List.of(new SymbolCountCheck("bytecode.test.tryassignifunset", 1)),
        false, false, false);
  }
  @Override protected boolean addDebugInstrumentation() { return false; }
}

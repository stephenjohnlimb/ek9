package org.ek9lang.compiler.bytecode;
import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

class SwitchAssignmentIfUnsetTest extends AbstractBytecodeGenerationTest {
  public SwitchAssignmentIfUnsetTest() {
    super("/examples/bytecodeGeneration/switchAssignmentIfUnset",
        List.of(new SymbolCountCheck("bytecode.test.switchassignifunset", 1)),
        false, false, false);
  }
  @Override protected boolean addDebugInstrumentation() { return false; }
}

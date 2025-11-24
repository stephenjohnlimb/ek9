package org.ek9lang.compiler.bytecode;
import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

class ForRangeAssignmentIfUnsetTest extends AbstractBytecodeGenerationTest {
  public ForRangeAssignmentIfUnsetTest() {
    super("/examples/bytecodeGeneration/forRangeAssignmentIfUnset",
        List.of(new SymbolCountCheck("bytecode.test.forrangeassignifunset", 1)),
        false, false, false);
  }
  @Override protected boolean addDebugInstrumentation() { return false; }
}

package org.ek9lang.compiler.bytecode;
import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

class ForInAssignmentIfUnsetTest extends AbstractBytecodeGenerationTest {
  public ForInAssignmentIfUnsetTest() {
    super("/examples/bytecodeGeneration/forInAssignmentIfUnset",
        List.of(new SymbolCountCheck("bytecode.test.forinassignifunset", 1)),
        false, false, false);
  }
  @Override protected boolean addDebugInstrumentation() { return false; }
}

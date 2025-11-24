package org.ek9lang.compiler.bytecode;
import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

class SwitchWithGuardAndControlTest extends AbstractBytecodeGenerationTest {
  public SwitchWithGuardAndControlTest() {
    super("/examples/bytecodeGeneration/switchWithGuardAndControl",
        List.of(new SymbolCountCheck("bytecode.test.switchguardcontrol", 1)),
        false, false, false);
  }
  @Override protected boolean addDebugInstrumentation() { return false; }
}

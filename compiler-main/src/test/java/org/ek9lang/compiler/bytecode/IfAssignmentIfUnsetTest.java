package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for if statement with assignment if unset (:=?) without condition.
 */
class IfAssignmentIfUnsetTest extends AbstractBytecodeGenerationTest {
  public IfAssignmentIfUnsetTest() {
    super("/examples/bytecodeGeneration/ifAssignmentIfUnset",
        List.of(new SymbolCountCheck("bytecode.test", 1)),
        false, false, false);
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;
  }
}

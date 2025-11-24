package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for while loop with assignment if unset (:=?).
 */
class WhileAssignmentIfUnsetTest extends AbstractBytecodeGenerationTest {
  public WhileAssignmentIfUnsetTest() {
    super("/examples/bytecodeGeneration/whileAssignmentIfUnset",
        List.of(new SymbolCountCheck("bytecode.test.whileassignifunset", 1)),
        false, false, false);  // showBytecode=false - @BYTECODE directive in place
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;
  }
}

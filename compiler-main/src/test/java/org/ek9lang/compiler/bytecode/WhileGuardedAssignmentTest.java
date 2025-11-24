package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for while loop with guarded assignment (?=).
 */
class WhileGuardedAssignmentTest extends AbstractBytecodeGenerationTest {
  public WhileGuardedAssignmentTest() {
    super("/examples/bytecodeGeneration/whileGuardedAssignment",
        List.of(new SymbolCountCheck("bytecode.test.whileguardedassign", 1)),
        false, false, false);  // showBytecode=false - @BYTECODE directive in place
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;
  }
}

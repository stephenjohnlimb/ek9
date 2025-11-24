package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for while loop with assignment guard (:=).
 */
class WhileAssignmentGuardTest extends AbstractBytecodeGenerationTest {
  public WhileAssignmentGuardTest() {
    super("/examples/bytecodeGeneration/whileAssignmentGuard",
        List.of(new SymbolCountCheck("bytecode.test.whileassignguard", 1)),
        false, false, false);  // showBytecode=false - @BYTECODE directive in place
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;
  }
}

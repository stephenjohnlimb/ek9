package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation and execution for while loop with guarded assignment (?=).
 */
class WhileGuardedAssignmentTest extends AbstractExecutableBytecodeTest {
  public WhileGuardedAssignmentTest() {
    super("/examples/bytecodeGeneration/whileGuardedAssignment",
        "bytecode.test.whileguardedassign",
        "WhileGuardedAssignment",
        List.of(new SymbolCountCheck("bytecode.test.whileguardedassign", 1)));
  }
}

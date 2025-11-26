package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation and execution for do-while loop with guarded assignment (?=).
 */
class DoWhileGuardedAssignmentTest extends AbstractExecutableBytecodeTest {
  public DoWhileGuardedAssignmentTest() {
    super("/examples/bytecodeGeneration/doWhileGuardedAssignment",
        "bytecode.test.dowhileguardedassign",
        "DoWhileGuardedAssignment",
        List.of(new SymbolCountCheck("bytecode.test.dowhileguardedassign", 1)));
  }
}

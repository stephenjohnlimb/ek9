package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation and execution for for-in loop with guarded assignment (?=).
 */
class ForInGuardedAssignmentTest extends AbstractExecutableBytecodeTest {
  public ForInGuardedAssignmentTest() {
    super("/examples/bytecodeGeneration/forInGuardedAssignment",
        "bytecode.test.foringuardedassign",
        "ForInGuardedAssignment",
        List.of(new SymbolCountCheck("bytecode.test.foringuardedassign", 1)));
  }
}

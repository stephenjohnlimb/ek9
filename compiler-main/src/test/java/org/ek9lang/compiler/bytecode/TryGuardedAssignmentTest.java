package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation and execution for try with guarded assignment (?=).
 */
class TryGuardedAssignmentTest extends AbstractExecutableBytecodeTest {
  public TryGuardedAssignmentTest() {
    super("/examples/bytecodeGeneration/tryGuardedAssignment",
        "bytecode.test.tryguardedassign",
        "TryGuardedAssignment",
        List.of(new SymbolCountCheck("bytecode.test.tryguardedassign", 1)));
  }
}

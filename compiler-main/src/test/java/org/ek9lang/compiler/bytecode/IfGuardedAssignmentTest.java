package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation and execution for if statement with guarded assignment (?=) without condition.
 */
class IfGuardedAssignmentTest extends AbstractExecutableBytecodeTest {
  public IfGuardedAssignmentTest() {
    super("/examples/bytecodeGeneration/ifGuardedAssignment",
        "bytecode.test",
        "IfGuardedAssignment",
        List.of(new SymbolCountCheck("bytecode.test", 1)));
  }
}

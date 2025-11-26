package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation and execution for for-range loop with guarded assignment (?=).
 */
class ForRangeGuardedAssignmentTest extends AbstractExecutableBytecodeTest {
  public ForRangeGuardedAssignmentTest() {
    super("/examples/bytecodeGeneration/forRangeGuardedAssignment",
        "bytecode.test.forrangeguardedassign",
        "ForRangeGuardedAssignment",
        List.of(new SymbolCountCheck("bytecode.test.forrangeguardedassign", 1)));
  }
}

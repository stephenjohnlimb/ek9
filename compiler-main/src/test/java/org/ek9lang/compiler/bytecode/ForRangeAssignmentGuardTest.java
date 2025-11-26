package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation and execution for for-range loop with assignment guard (:=).
 */
class ForRangeAssignmentGuardTest extends AbstractExecutableBytecodeTest {
  public ForRangeAssignmentGuardTest() {
    super("/examples/bytecodeGeneration/forRangeAssignmentGuard",
        "bytecode.test.forrangeassignguard",
        "ForRangeAssignmentGuard",
        List.of(new SymbolCountCheck("bytecode.test.forrangeassignguard", 1)));
  }
}

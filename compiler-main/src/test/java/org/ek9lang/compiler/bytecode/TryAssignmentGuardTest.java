package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation and execution for try with assignment guard (:=).
 */
class TryAssignmentGuardTest extends AbstractExecutableBytecodeTest {
  public TryAssignmentGuardTest() {
    super("/examples/bytecodeGeneration/tryAssignmentGuard",
        "bytecode.test.tryassignguard",
        "TryAssignmentGuard",
        List.of(new SymbolCountCheck("bytecode.test.tryassignguard", 1)));
  }
}

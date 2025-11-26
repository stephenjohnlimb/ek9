package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation and execution for while loop with assignment guard (:=).
 */
class WhileAssignmentGuardTest extends AbstractExecutableBytecodeTest {
  public WhileAssignmentGuardTest() {
    super("/examples/bytecodeGeneration/whileAssignmentGuard",
        "bytecode.test.whileassignguard",
        "WhileAssignmentGuard",
        List.of(new SymbolCountCheck("bytecode.test.whileassignguard", 1)));
  }
}

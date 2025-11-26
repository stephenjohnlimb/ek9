package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation and execution for do-while loop with assignment guard (:=).
 */
class DoWhileAssignmentGuardTest extends AbstractExecutableBytecodeTest {
  public DoWhileAssignmentGuardTest() {
    super("/examples/bytecodeGeneration/doWhileAssignmentGuard",
        "bytecode.test.dowhileassignguard",
        "DoWhileAssignmentGuard",
        List.of(new SymbolCountCheck("bytecode.test.dowhileassignguard", 1)));
  }
}

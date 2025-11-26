package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation and execution for for-in loop with assignment guard (:=).
 */
class ForInAssignmentGuardTest extends AbstractExecutableBytecodeTest {
  public ForInAssignmentGuardTest() {
    super("/examples/bytecodeGeneration/forInAssignmentGuard",
        "bytecode.test.forinassignguard",
        "ForInAssignmentGuard",
        List.of(new SymbolCountCheck("bytecode.test.forinassignguard", 1)));
  }
}

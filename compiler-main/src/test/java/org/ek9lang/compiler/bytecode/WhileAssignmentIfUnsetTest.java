package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation and execution for while loop with assignment if unset (:=?).
 */
class WhileAssignmentIfUnsetTest extends AbstractExecutableBytecodeTest {
  public WhileAssignmentIfUnsetTest() {
    super("/examples/bytecodeGeneration/whileAssignmentIfUnset",
        "bytecode.test.whileassignifunset",
        "WhileAssignmentIfUnset",
        List.of(new SymbolCountCheck("bytecode.test.whileassignifunset", 1)));
  }
}

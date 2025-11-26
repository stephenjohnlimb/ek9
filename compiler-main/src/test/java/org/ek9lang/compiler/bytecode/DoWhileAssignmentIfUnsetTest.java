package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation and execution for do-while loop with assignment if unset (:=?).
 */
class DoWhileAssignmentIfUnsetTest extends AbstractExecutableBytecodeTest {
  public DoWhileAssignmentIfUnsetTest() {
    super("/examples/bytecodeGeneration/doWhileAssignmentIfUnset",
        "bytecode.test.dowhileassignifunset",
        "DoWhileAssignmentIfUnset",
        List.of(new SymbolCountCheck("bytecode.test.dowhileassignifunset", 1)));
  }
}

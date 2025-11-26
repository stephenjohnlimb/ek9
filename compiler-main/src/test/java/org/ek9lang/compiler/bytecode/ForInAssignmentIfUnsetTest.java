package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation and execution for for-in loop with assignment if unset (:=?).
 */
class ForInAssignmentIfUnsetTest extends AbstractExecutableBytecodeTest {
  public ForInAssignmentIfUnsetTest() {
    super("/examples/bytecodeGeneration/forInAssignmentIfUnset",
        "bytecode.test.forinassignifunset",
        "ForInAssignmentIfUnset",
        List.of(new SymbolCountCheck("bytecode.test.forinassignifunset", 1)));
  }
}

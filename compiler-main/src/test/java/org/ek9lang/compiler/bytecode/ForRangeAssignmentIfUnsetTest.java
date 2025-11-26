package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation and execution for for-range loop with assignment if unset (:=?).
 */
class ForRangeAssignmentIfUnsetTest extends AbstractExecutableBytecodeTest {
  public ForRangeAssignmentIfUnsetTest() {
    super("/examples/bytecodeGeneration/forRangeAssignmentIfUnset",
        "bytecode.test.forrangeassignifunset",
        "ForRangeAssignmentIfUnset",
        List.of(new SymbolCountCheck("bytecode.test.forrangeassignifunset", 1)));
  }
}

package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation and execution for try with assignment if unset (:=?).
 */
class TryAssignmentIfUnsetTest extends AbstractExecutableBytecodeTest {
  public TryAssignmentIfUnsetTest() {
    super("/examples/bytecodeGeneration/tryAssignmentIfUnset",
        "bytecode.test.tryassignifunset",
        "TryAssignmentIfUnset",
        List.of(new SymbolCountCheck("bytecode.test.tryassignifunset", 1)));
  }
}

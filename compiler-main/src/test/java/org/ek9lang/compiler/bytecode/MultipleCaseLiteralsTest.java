package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation and execution for switch with multiple case expressions (OR logic).
 */
class MultipleCaseLiteralsTest extends AbstractExecutableBytecodeTest {
  public MultipleCaseLiteralsTest() {
    super("/examples/bytecodeGeneration/multipleCaseLiterals",
        "bytecode.test.multiple",
        "MultipleCaseLiterals",
        List.of(new SymbolCountCheck("bytecode.test.multiple", 1)));
  }
}

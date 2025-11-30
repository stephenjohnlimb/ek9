package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for unary operators.
 * Tests: is empty, length, ++ (increment), -- (decrement)
 */
class UnaryOperatorsTest extends AbstractExecutableBytecodeTest {

  public UnaryOperatorsTest() {
    super("/examples/bytecodeGeneration/unaryOperators",
        "bytecode.test",
        "UnaryOperators",
        List.of(new SymbolCountCheck("bytecode.test", 1)));
  }
}

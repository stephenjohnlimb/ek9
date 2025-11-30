package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for Bits bitwise operators.
 * Tests: << (shift left), >> (shift right)
 */
class BitwiseOperatorsTest extends AbstractExecutableBytecodeTest {

  public BitwiseOperatorsTest() {
    super("/examples/bytecodeGeneration/bitwiseOperators",
        "bytecode.test",
        "BitwiseOperators",
        List.of(new SymbolCountCheck("bytecode.test", 1)));
  }
}

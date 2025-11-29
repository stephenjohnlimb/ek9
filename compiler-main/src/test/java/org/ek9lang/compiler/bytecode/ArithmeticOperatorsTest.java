package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for Integer arithmetic operators.
 * Tests: + (addition), - (subtraction), * (multiplication), / (division), - (negation)
 */
class ArithmeticOperatorsTest extends AbstractExecutableBytecodeTest {

  public ArithmeticOperatorsTest() {
    super("/examples/bytecodeGeneration/arithmeticOperators",
        "bytecode.test",
        "ArithmeticOperators",
        List.of(new SymbolCountCheck("bytecode.test", 1)));
  }
}

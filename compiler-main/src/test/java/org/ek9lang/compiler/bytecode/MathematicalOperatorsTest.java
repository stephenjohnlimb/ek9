package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for Integer mathematical operators.
 * Tests: mod (modulo), rem (remainder), ^ (power), |x| (absolute), √ (square root), ! (factorial)
 */
class MathematicalOperatorsTest extends AbstractExecutableBytecodeTest {

  public MathematicalOperatorsTest() {
    super("/examples/bytecodeGeneration/mathematicalOperators",
        "bytecode.test",
        "MathematicalOperators",
        List.of(new SymbolCountCheck("bytecode.test", 1)));
  }
}

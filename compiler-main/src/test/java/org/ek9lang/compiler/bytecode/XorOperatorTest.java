package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test the byte code generation of the 'xor' Operator.
 */
class XorOperatorTest extends AbstractBytecodeGenerationTest {

  public XorOperatorTest() {
    //Each bytecode test gets its own directory for parallel execution safety
    //Module name: bytecode.test, expected symbol count: 1 (the program)
    super("/examples/bytecodeGeneration/xorOperator",
        List.of(new SymbolCountCheck("bytecode.test", 1)),
        false, false, false);
  }
}

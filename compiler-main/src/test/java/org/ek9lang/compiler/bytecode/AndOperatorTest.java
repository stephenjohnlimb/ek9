package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test the byte code generation of the 'and' Operator.
 */
class AndOperatorTest extends AbstractExecutableBytecodeTest {

  public AndOperatorTest() {
    super("/examples/bytecodeGeneration/andOperator",
        "bytecode.test",
        "AndOperator",
        List.of(new SymbolCountCheck("bytecode.test", 1)));
  }
}

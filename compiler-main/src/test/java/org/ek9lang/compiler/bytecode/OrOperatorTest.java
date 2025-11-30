package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test the byte code generation of the 'or' Operator.
 */
class OrOperatorTest extends AbstractExecutableBytecodeTest {

  public OrOperatorTest() {
    super("/examples/bytecodeGeneration/orOperator",
        "bytecode.test",
        "OrOperator",
        List.of(new SymbolCountCheck("bytecode.test", 1)));
  }
}

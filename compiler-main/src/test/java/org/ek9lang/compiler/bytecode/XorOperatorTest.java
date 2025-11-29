package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test the byte code generation of the 'xor' Operator.
 */
class XorOperatorTest extends AbstractExecutableBytecodeTest {

  public XorOperatorTest() {
    super("/examples/bytecodeGeneration/xorOperator",
        "bytecode.test",
        "XorOperator",
        List.of(new SymbolCountCheck("bytecode.test", 1)));
  }
}

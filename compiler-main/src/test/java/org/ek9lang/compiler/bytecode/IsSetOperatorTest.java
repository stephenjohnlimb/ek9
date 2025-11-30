package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test the byte code generation of the isSet Operator.
 */
class IsSetOperatorTest extends AbstractExecutableBytecodeTest {

  public IsSetOperatorTest() {
    super("/examples/bytecodeGeneration/isSetOperator",
        "bytecode.test",
        "IsSetOperator",
        List.of(new SymbolCountCheck("bytecode.test", 1)));
  }
}

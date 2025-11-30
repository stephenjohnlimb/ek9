package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test the byte code generation of the 'not' (~) Operator.
 * Tests both ~ and 'not' keyword syntax.
 */
class NotOperatorTest extends AbstractExecutableBytecodeTest {

  public NotOperatorTest() {
    super("/examples/bytecodeGeneration/notOperator",
        "bytecode.test",
        "NotOperator",
        List.of(new SymbolCountCheck("bytecode.test", 1)));
  }
}

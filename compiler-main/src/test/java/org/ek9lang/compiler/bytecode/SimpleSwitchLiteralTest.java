package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation and execution for simple switch with literal integer comparisons.
 */
class SimpleSwitchLiteralTest extends AbstractExecutableBytecodeTest {
  public SimpleSwitchLiteralTest() {
    super("/examples/bytecodeGeneration/simpleSwitchLiteral",
        "bytecode.test",
        "SimpleSwitchLiteral",
        List.of(new SymbolCountCheck("bytecode.test", 1)));
  }
}

package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for if/else statements.
 */
class IfElseStatementTest extends AbstractBytecodeGenerationTest {
  public IfElseStatementTest() {
    super("/examples/bytecodeGeneration/ifElseStatement",
        List.of(new SymbolCountCheck("bytecode.test", 1)),
        false, false, false);
  }
}
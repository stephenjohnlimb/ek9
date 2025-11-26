package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation and execution for if/else statements.
 * Tests conditional branching with else clause.
 */
class IfElseStatementTest extends AbstractExecutableBytecodeTest {
  public IfElseStatementTest() {
    super("/examples/bytecodeGeneration/ifElseStatement",
        "bytecode.test",
        "IfElseStatement",
        List.of(new SymbolCountCheck("bytecode.test", 1)));
  }
}
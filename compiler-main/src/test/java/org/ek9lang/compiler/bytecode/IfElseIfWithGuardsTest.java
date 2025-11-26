package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation and execution for if/else-if with multiple guards.
 */
class IfElseIfWithGuardsTest extends AbstractExecutableBytecodeTest {
  public IfElseIfWithGuardsTest() {
    super("/examples/bytecodeGeneration/ifElseIfWithGuards",
        "bytecode.test",
        "IfElseIfWithGuards",
        List.of(new SymbolCountCheck("bytecode.test", 1)));
  }
}

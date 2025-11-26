package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation and execution for if/else-if/else chains.
 */
class IfElseIfChainTest extends AbstractExecutableBytecodeTest {
  public IfElseIfChainTest() {
    super("/examples/bytecodeGeneration/ifElseIfChain",
        "bytecode.test",
        "IfElseIfChain",
        List.of(new SymbolCountCheck("bytecode.test", 1)));
  }
}
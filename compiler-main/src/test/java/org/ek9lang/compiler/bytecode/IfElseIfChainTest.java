package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for if/else-if/else chains.
 */
class IfElseIfChainTest extends AbstractBytecodeGenerationTest {
  public IfElseIfChainTest() {
    super("/examples/bytecodeGeneration/ifElseIfChain",
        List.of(new SymbolCountCheck("bytecode.test", 1)),
        false, false, true);
  }
}
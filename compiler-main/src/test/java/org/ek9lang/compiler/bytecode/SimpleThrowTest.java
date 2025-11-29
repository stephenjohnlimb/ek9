package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for simple throw statement.
 * Validates that THROW instruction generates correct ATHROW bytecode.
 */
class SimpleThrowTest extends AbstractExecutableBytecodeTest {

  public SimpleThrowTest() {
    super("/examples/bytecodeGeneration/simpleThrow",
        "bytecode.test",
        "TestSimpleThrow",
        List.of(new SymbolCountCheck("bytecode.test", 1)));
  }
}

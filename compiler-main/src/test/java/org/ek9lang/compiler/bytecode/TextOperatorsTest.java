package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for String text operators.
 * Tests: contains, matches
 */
class TextOperatorsTest extends AbstractExecutableBytecodeTest {

  public TextOperatorsTest() {
    super("/examples/bytecodeGeneration/textOperators",
        "bytecode.test",
        "TextOperators",
        List.of(new SymbolCountCheck("bytecode.test", 1)));
  }
}

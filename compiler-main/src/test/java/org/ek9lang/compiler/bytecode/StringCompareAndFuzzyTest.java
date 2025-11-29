package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for String compare and fuzzy operators.
 * Tests: <=> (compare), <~> (fuzzy)
 */
class StringCompareAndFuzzyTest extends AbstractExecutableBytecodeTest {

  public StringCompareAndFuzzyTest() {
    super("/examples/bytecodeGeneration/stringCompareAndFuzzy",
        "bytecode.test",
        "StringCompareAndFuzzy",
        List.of(new SymbolCountCheck("bytecode.test", 1)));
  }
}

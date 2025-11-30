package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for Integer comparison operators.
 * Tests: ==, <>, <, <=, >, >=
 */
class ComparisonOperatorsTest extends AbstractExecutableBytecodeTest {

  public ComparisonOperatorsTest() {
    super("/examples/bytecodeGeneration/comparisonOperators",
        "bytecode.test",
        "ComparisonOperators",
        List.of(new SymbolCountCheck("bytecode.test", 1)));
  }
}

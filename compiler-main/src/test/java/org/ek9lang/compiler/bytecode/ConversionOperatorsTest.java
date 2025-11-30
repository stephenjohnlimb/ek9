package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for type conversion operators.
 * Tests: #? (hashcode), $ (string), #^ (promote), $$ (JSON)
 */
class ConversionOperatorsTest extends AbstractExecutableBytecodeTest {

  public ConversionOperatorsTest() {
    super("/examples/bytecodeGeneration/conversionOperators",
        "bytecode.test",
        "ConversionOperators",
        List.of(new SymbolCountCheck("bytecode.test", 1)));
  }
}

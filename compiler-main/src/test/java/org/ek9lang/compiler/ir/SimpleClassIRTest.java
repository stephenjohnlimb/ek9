package org.ek9lang.compiler.ir;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Temporary test to examine IR generation for SimpleClass.
 * This validates whether field STORE instructions are correctly generated in IR.
 */
class SimpleClassIRTest extends AbstractIRGenerationTest {

  public SimpleClassIRTest() {
    // Module name: bytecode.test
    // Expected symbols: 1 program + 1 class = 2
    super("/examples/bytecodeGeneration/simpleClass",
        List.of(new SymbolCountCheck("bytecode.test", 2)),
        false, false, false);
  }
}

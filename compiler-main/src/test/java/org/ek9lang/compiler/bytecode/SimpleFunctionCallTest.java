package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Bytecode test for simple function calls.
 * Tests calling functions with parameters and return values from a program.
 */
class SimpleFunctionCallTest extends AbstractExecutableBytecodeTest {

  public SimpleFunctionCallTest() {
    super("/examples/bytecodeGeneration/simpleFunctionCall",
        "bytecode.test.simplefunctioncall",
        "SimpleFunctionCall",
        List.of(new SymbolCountCheck("bytecode.test.simplefunctioncall", 3)));
  }

}

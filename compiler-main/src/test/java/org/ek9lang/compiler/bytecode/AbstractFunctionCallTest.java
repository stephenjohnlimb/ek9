package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Bytecode test for abstract function calls.
 * Tests:
 * - Abstract functions generate abstract classes (no INSTANCE singleton)
 * - Concrete functions extending abstract have INSTANCE and working _call
 * - Programs can call concrete functions
 * - Polymorphic usage via abstract type works
 */
class AbstractFunctionCallTest extends AbstractExecutableBytecodeTest {

  public AbstractFunctionCallTest() {
    super("/examples/bytecodeGeneration/abstractFunctionCall",
        "bytecode.test.abstractfunctioncall",
        "AbstractFunctionCall",
        List.of(new SymbolCountCheck("bytecode.test.abstractfunctioncall", 3)));
  }

}

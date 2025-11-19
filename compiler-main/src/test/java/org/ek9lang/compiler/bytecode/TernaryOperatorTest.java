package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for ternary operator (condition <- thenValue : elseValue).
 * Validates JVM bytecode for EK9's expression form of if/else.
 */
class TernaryOperatorTest extends AbstractBytecodeGenerationTest {

  public TernaryOperatorTest() {
    super("/examples/bytecodeGeneration/ternaryOperator",
        List.of(new SymbolCountCheck("bytecode.test", 1)),
        false, false, false);
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;
  }
}

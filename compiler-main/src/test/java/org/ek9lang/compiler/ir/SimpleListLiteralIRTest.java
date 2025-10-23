package org.ek9lang.compiler.ir;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test IR generation for simple list literal expressions.
 * Tests the syntax: ["element1", "element2", ...]
 */
class SimpleListLiteralIRTest extends AbstractIRGenerationTest {

  public SimpleListLiteralIRTest() {
    super("/examples/irGeneration/expressions",
        List.of(new SymbolCountCheck("expressions", 1)),
        false, false, false);
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;
  }
}

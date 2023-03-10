package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.main.phases.CompilationPhase;
import org.ek9lang.compiler.symbol.support.SymbolSearchMapFunction;
import org.junit.jupiter.api.Test;

/**
 * .
 */
class GenericTypesCompilationTest extends FullCompilationTest {

  public GenericTypesCompilationTest() {
    super("/examples/genericTypes");
  }

  private final SymbolSearchMapFunction mapFunction = new SymbolSearchMapFunction();

  @Test
  void testPhasedDevelopment() {
    testToPhase(CompilationPhase.EXPLICIT_TYPE_SYMBOL_DEFINITION);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertTrue(compilationResult);
    assertEquals(0, numberOfErrors);
  }
}

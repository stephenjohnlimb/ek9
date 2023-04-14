package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.main.phases.CompilationPhase;
import org.ek9lang.compiler.symbol.support.SymbolCountCheck;
import org.junit.jupiter.api.Test;

/**
 * Tests all construct types in basic form, then checks they can be resolved.
 */
class ExamplesJustResolutionTest extends FullCompilationTest {

  public ExamplesJustResolutionTest() {
    super("/examples/justResolution");
  }

  @Test
  void testPhasedDevelopment() {
    testToPhase(CompilationPhase.EXPLICIT_TYPE_SYMBOL_DEFINITION);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertTrue(compilationResult);
    assertEquals(0, numberOfErrors);

    var mainModuleName = "just.resolution.items";
    var referencingModuleName = "just.reference.items";

    new SymbolCountCheck(2,mainModuleName, 4).test(program);
    new SymbolCountCheck(1,referencingModuleName, 1).test(program);

  }
}

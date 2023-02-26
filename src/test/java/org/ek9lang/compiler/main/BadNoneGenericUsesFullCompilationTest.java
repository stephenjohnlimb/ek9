package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.main.phases.CompilationPhase;
import org.ek9lang.compiler.symbol.support.SymbolSearchMapFunction;
import org.junit.jupiter.api.Test;

/**
 * Just tests bad generic uses of generics.
 */
class BadNoneGenericUsesFullCompilationTest extends FullCompilationTest {

  public BadNoneGenericUsesFullCompilationTest() {
    super("/examples/parseButFailCompile/badNoneGenericUses");
  }

  private final SymbolSearchMapFunction mappingFunction = new SymbolSearchMapFunction();

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.EXPLICIT_TYPE_SYMBOL_DEFINITION);
  }

  @Override
  protected void assertFinalResults(final boolean compilationResult, final int numberOfErrors,
                                    final CompilableProgram program) {
    assertFalse(compilationResult);
    assertEquals(4, numberOfErrors);
  }
}

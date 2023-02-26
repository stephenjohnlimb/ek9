package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.main.phases.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Just tests bad class modifier usage.
 */
class BadClassModifiersFullCompilationTest extends FullCompilationTest {

  public BadClassModifiersFullCompilationTest() {
    super("/examples/parseButFailCompile/badClassMethods");
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.EXPLICIT_TYPE_SYMBOL_DEFINITION);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors,
                                    CompilableProgram program) {
    assertFalse(compilationResult);
    assertEquals(7, numberOfErrors);
    var alpha = program.getParsedModules("bad.classmodifier.use");
    assertNotNull(alpha);
  }
}

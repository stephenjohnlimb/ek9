package org.ek9lang.compiler.phase1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.junit.jupiter.api.Test;

/**
 * Just tests duplicated symbols but in different files.
 * For this test we don't use @directives -cause the multi-thread nature of N files means you cannot
 * guarantee the order of failure.
 */
class BadDuplicatedSymbolsDifferentFilesTest extends PhasesTest {

  public BadDuplicatedSymbolsDifferentFilesTest() {
    super("/examples/parseButFailCompile/duplicatedInDifferentFiles",
        List.of("duplications"));
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.SYMBOL_DEFINITION);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertFalse(compilationResult);
    assertEquals(3, numberOfErrors);
  }
}

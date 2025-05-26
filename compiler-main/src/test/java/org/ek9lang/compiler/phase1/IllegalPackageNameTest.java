package org.ek9lang.compiler.phase1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilableSource;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.PhasesTest;
import org.ek9lang.core.SharedThreadContext;
import org.junit.jupiter.api.Test;

/**
 * Just tests for use of prohibited package names.
 */
class IllegalPackageNameTest extends PhasesTest {


  public IllegalPackageNameTest() {
    super("/examples/parseButFailCompile/illegalPackageNames");
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.SYMBOL_DEFINITION);
  }

  @Override
  protected void compilationPhaseCompleted(final CompilationPhase phase, final CompilableSource source,
                                           final SharedThreadContext<CompilableProgram> sharedCompilableProgram) {

    if (phase == CompilationPhase.SYMBOL_DEFINITION) {
      assertTrue(source.getErrorListener().hasErrors());
      assertEquals(ErrorListener.SemanticClassification.INVALID_MODULE_NAME,
          source.getErrorListener().getErrors().next().getSemanticClassification());
    }
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertFalse(compilationResult);
    assertEquals(2, numberOfErrors);
  }
}

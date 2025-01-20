package org.ek9lang.compiler.phase1;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Iterator;
import java.util.List;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilableSource;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.PhasesTest;
import org.ek9lang.core.SharedThreadContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Just tests bad directives. i.e. the very thing we use for testing - needs to be checked.
 * A bit intricate because we're testing errors of error processing.
 * This is a bit fragile as it embeds line numbers in here, so take car if changing code.
 */
class BadDirectivesTest extends PhasesTest {

  private final List<Integer> badErrorLineNumbers
      = List.of(10, 14, 18, 23, 28, 36);
  private final List<Integer> badDirectiveErrorLineNumbers
      = List.of(10, 14, 18, 23, 28, 35);
  private final List<Integer> badResolutionLineNumbers
      = List.of(6, 10, 14, 18, 22, 26, 46, 51, 57, 75);
  private final List<Integer> badDirectiveResolutionErrorLineNumbers
      = List.of(31, 36, 41, 64, 82, 90, 98, 105, 123, 133);

  public BadDirectivesTest() {
    super("/examples/parseButFailCompile/badDirectives", List.of(),
        false, true, false);
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.SYMBOL_DEFINITION);
  }

  protected boolean errorOnDirectiveErrors() {
    return false;
  }

  protected void compilationPhaseCompleted(final CompilationPhase phase, final CompilableSource source,
                                           final SharedThreadContext<CompilableProgram> sharedCompilableProgram) {

    if (phase == CompilationPhase.SYMBOL_DEFINITION) {
      if (source.getFileName().endsWith("badErrorDirectives.ek9")) {
        assertAllErrors(source.getErrorListener().getErrors(), badErrorLineNumbers);
        assertAllErrors(source.getErrorListener().getDirectiveErrors(), badDirectiveErrorLineNumbers);
      } else if (source.getFileName().endsWith("badResolutionDirectives.ek9")) {
        assertAllErrors(source.getErrorListener().getErrors(), badResolutionLineNumbers);
        assertAllErrors(source.getErrorListener().getDirectiveErrors(), badDirectiveResolutionErrorLineNumbers);
      }
    }
  }

  private void assertAllErrors(final Iterator<ErrorListener.ErrorDetails> iterator, final List<Integer> expected) {
    while (iterator.hasNext()) {
      int lineNumber = iterator.next().getLineNumber();
      if (lineNumber != 0 && !expected.contains(lineNumber)) {
        //Because those on line 0 are just summaries.
        Assertions.fail("Expecting an error on line number [" + lineNumber + "] in " + expected);
      }
    }
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertFalse(compilationResult);

    assertFalse(program.getParsedModules("bad.error.directives").isEmpty());
    assertFalse(program.getParsedModules("bad.resolution.directives").isEmpty());
  }
}

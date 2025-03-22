package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.ek9lang.compiler.search.TypeSymbolSearch;
import org.junit.jupiter.api.Test;

/**
 * Just tests being worked on.
 * Sometimes it's good to have a small set of tests to focus on then when done put them in the appropriate space.
 */
class WorkingAreaTest extends PhasesTest {

  public WorkingAreaTest() {
    super("/examples/parseButFailCompile/workingarea");
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.SIMPLE_IR_GENERATION);
  }

  @Override
  protected void assertFinalResults(final boolean compilationResult, final int numberOfErrors,
                                    final CompilableProgram program) {
    assertTrue(compilationResult);
    var resolvedProgram = program.resolveFromModule("introduction", new TypeSymbolSearch("HelloWorld"));

    //showAllSymbolsInAllModules.accept(new SharedThreadContext<>(program));
  }
}

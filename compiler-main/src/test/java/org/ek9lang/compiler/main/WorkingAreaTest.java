package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.NodePrinter;
import org.ek9lang.compiler.common.PhasesTest;
import org.ek9lang.compiler.search.TypeSymbolSearch;
import org.junit.jupiter.api.Test;

/**
 * Just tests being worked on.
 * Sometimes it's good to have a small set of tests to focus on then when done put them in the appropriate space.
 */
class WorkingAreaTest extends PhasesTest {

  public WorkingAreaTest() {
    super("/examples/parseButFailCompile/workingarea", false, false);
  }

  @Test
  void testPhaseDevelopment() {
    //Need to clear out any existing targets (like clean) for this unit test.
    ek9Workspace.getSources().stream().findFirst().ifPresent(source -> {
      fileHandling.cleanEk9DirectoryStructureFor(source.getFileName(), targetArchitecture);
    });

    testToPhase(CompilationPhase.CODE_GENERATION_AGGREGATES);
  }

  @Override
  protected void assertFinalResults(final boolean compilationResult, final int numberOfErrors,
                                    final CompilableProgram program) {
    assertTrue(compilationResult);
    var resolvedProgram = program.resolveFromModule("introduction", new TypeSymbolSearch("HelloWorld"));
    assertNotNull(resolvedProgram);

    final var printer = new NodePrinter();
    program.getIRModules("introduction").forEach(irModule -> {
      irModule.getConstructs().forEach(printer::visit);
    });

  }
}

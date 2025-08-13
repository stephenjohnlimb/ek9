package org.ek9lang.compiler.ir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.ek9lang.compiler.support.SymbolCountCheck;
import org.junit.jupiter.api.Test;

class AbstractIRGenerationTest extends PhasesTest {

  private final List<SymbolCountCheck> expectedSymbols;
  public AbstractIRGenerationTest(final String fromResourcesDirectory,
                                  final List<SymbolCountCheck> expectedSymbols, final boolean verbose,
                                  final boolean muteReportedErrors) {
    super(fromResourcesDirectory, verbose, muteReportedErrors);
    this.expectedSymbols = expectedSymbols;

  }

  /**
   * For this test I've enabled debugging output in the IR and Code generation (once implemented).
   *
   * @return true enable debug instrumentation.
   */
  @Override
  protected boolean addDebugInstrumentation() {
    return true;
  }

  @Test
  void testPhaseDevelopment() {
    //Need to clear out any existing targets (like clean) for this unit test.
    ek9Workspace.getSources().stream().findFirst()
        .ifPresent(source -> fileHandling.cleanEk9DirectoryStructureFor(source.getFileName(), targetArchitecture));

    testToPhase(CompilationPhase.IR_GENERATION);
  }

  @Override
  protected void assertFinalResults(final boolean compilationResult, final int numberOfErrors,
                                    final CompilableProgram program) {
    assertTrue(compilationResult);
    assertEquals(0, numberOfErrors);
    expectedSymbols.forEach(check -> check.test(program));

  }
}

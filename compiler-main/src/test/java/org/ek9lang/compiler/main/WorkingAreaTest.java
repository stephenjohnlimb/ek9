package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.NodePrinter;
import org.ek9lang.compiler.common.PhasesTest;
import org.junit.jupiter.api.Test;

/**
 * Just tests being worked on.
 * Sometimes it's good to have a small set of tests to focus on then when done put them in the appropriate space.
 */
class WorkingAreaTest extends PhasesTest {

  public WorkingAreaTest() {
    super("/examples/parseButFailCompile/workingarea", false, false);
  }

  /**
   * For this test I've like to enable debugging output in the IR and Code generation (once implemented).
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

    final var output = new ByteArrayOutputStream();
    try (final var printWriter = new PrintWriter(output)) {
      final var printer = new NodePrinter(printWriter);
      program
          .getIRModules("exceptionHandling")
          .forEach(irModule -> irModule.getConstructs().forEach(printer::visit));
    } catch (Exception _) {
      fail("Failed to produce output.");
    }
    System.out.println(output);
  }
}

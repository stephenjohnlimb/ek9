package org.ek9lang.compiler.ir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.NodePrinter;
import org.ek9lang.compiler.common.PhasesTest;
import org.ek9lang.compiler.support.SymbolCountCheck;
import org.junit.jupiter.api.Test;

abstract class AbstractIRGenerationTest extends PhasesTest {

  private final List<SymbolCountCheck> expectedSymbols;
  private final boolean showIR;
  public AbstractIRGenerationTest(final String fromResourcesDirectory,
                                  final List<SymbolCountCheck> expectedSymbols, final boolean verbose,
                                  final boolean muteReportedErrors, final boolean showIR) {
    super(fromResourcesDirectory, verbose, muteReportedErrors);
    this.expectedSymbols = expectedSymbols;
    this.showIR = showIR;
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
    if(showIR) {
      showIR(program);
    }

  }

  protected void showIR(final CompilableProgram program) {
    final var output = new ByteArrayOutputStream();
    try (final var printWriter = new PrintWriter(output)) {
      final var printer = new NodePrinter(printWriter);
      expectedSymbols
          .stream()
          .map(SymbolCountCheck::getForModuleName)
          .map(program::getIRModules)
          .flatMap(Collection::stream)
          .forEach(irModule -> irModule.getConstructs().forEach(printer::visit));
    } catch (Exception _) {
      fail("Failed to produce output.");
    }
    System.out.println(output);
  }
}

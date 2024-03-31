package org.ek9lang.compiler.common;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilableSource;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.CompilerFlags;
import org.ek9lang.compiler.Ek9Compiler;
import org.ek9lang.compiler.Workspace;
import org.ek9lang.compiler.config.FullPhaseSupplier;
import org.ek9lang.compiler.directives.DirectiveType;
import org.ek9lang.compiler.support.ShowAllSymbolsInAllModules;
import org.ek9lang.core.SharedThreadContext;
import org.junit.jupiter.api.Assertions;

/**
 * Abstract base for the range of different tests that are needed.
 */
public abstract class PhasesTest {

  private final Supplier<SharedThreadContext<CompilableProgram>> sharedContext = new CompilableProgramSuitable();

  private final Workspace ek9Workspace;

  private final ShowAllSymbolsInAllModules showAllSymbolsInAllModules = new ShowAllSymbolsInAllModules();

  private final List<String> expectedModules;

  //If you want to see timings set verbose true, and to see the actual errors set muteReportedErrors to false.
  //This is to that builds can be silent. But when developing you'll probably need to see the actual errors.
  private final CompilerReporter reporter;

  public PhasesTest(final String fromResourcesDirectory) {
    this(fromResourcesDirectory, List.of());
  }

  public PhasesTest(final String fromResourcesDirectory, final boolean verbose, final boolean muteReportedErrors) {
    this(fromResourcesDirectory, List.of(), verbose, muteReportedErrors);

  }

  public PhasesTest(final String fromResourcesDirectory,
                    final List<String> expectedModules,
                    final boolean muteReportedErrors) {
    this(fromResourcesDirectory, expectedModules, false, muteReportedErrors);
  }

  public PhasesTest(final String fromResourcesDirectory, final List<String> expectedModules) {
    this(fromResourcesDirectory, expectedModules, false, true);
  }

  public PhasesTest(final String fromResourcesDirectory, final List<String> expectedModules,
                    final boolean verbose, final boolean muteReportedErrors) {
    var workspaceLoader = new WorkSpaceFromResourceDirectoryFiles();
    this.expectedModules = expectedModules;
    ek9Workspace = workspaceLoader.apply(fromResourcesDirectory);
    this.reporter = new CompilerReporter(verbose, muteReportedErrors);
  }

  /**
   * Optionally test classes can implement this for precondition processing.
   */
  protected void assertPreConditions(CompilableProgram program) {
  }

  protected boolean errorOnDirectiveErrors() {
    return true;
  }

  protected abstract void assertFinalResults(final boolean compilationResult,
                                             final int numberOfErrors,
                                             final CompilableProgram program);

  private void checkFinalResults(final boolean compilationResult,
                                 final int numberOfErrors,
                                 final CompilableProgram program) {

    var hasErrorDirective = program.getParsedModuleNames()
        .stream()
        .map(program::getParsedModules)
        .flatMap(List::stream)
        .map(module -> module.getDirectives(DirectiveType.Error))
        .flatMap(List::stream)
        .findAny().isPresent();

    //Basically if there are any error directives it means ek9 source is expecting to fail compilation.
    //So we can assert that the compilation result is false, just by the fact the EK9 source has the @Error directive.
    if (hasErrorDirective) {
      assertFalse(compilationResult, "Expecting error directives presence to cause compilation failure");
    }

    //If modules have been supplied then check they are present and not empty
    expectedModules.forEach(moduleName -> assertFalse(program.getParsedModules(moduleName).isEmpty()));

    assertFinalResults(compilationResult, numberOfErrors, program);
  }

  /**
   * Override if the test needs to check any intermediate phase results.
   */
  protected void compilationPhaseCompleted(final CompilationPhase phase, final CompilableSource source,
                                           final SharedThreadContext<CompilableProgram> sharedCompilableProgram) {
  }

  private void checkCompilationPhase(final CompilationPhase phase, final CompilableSource source,
                                     final SharedThreadContext<CompilableProgram> sharedCompilableProgram) {
    compilationPhaseCompleted(phase, source, sharedCompilableProgram);
    //Now there should be no directive errors at all, else this test has failed.
    //As the directives have been added into the EK9 source for testing it means that the EK9 source
    //Now also contains the types and locations of the errors we are looking for.

    //Dump all the symbols for all modules.
    if (errorOnDirectiveErrors()) {
      if (source.getErrorListener().hasDirectiveErrors()) {
        System.out.println("Dumping all Symbols from all Modules");
        showAllSymbolsInAllModules.accept(sharedCompilableProgram);
      }
      assertFalse(source.getErrorListener().hasDirectiveErrors(), "There are '@' directives that have failed");
    }
  }

  protected void testToPhase(final CompilationPhase upToPhase) {
    //Just start with the basics and most on to the next phase one implemented.

    var sharedCompilableProgram = sharedContext.get();
    AtomicInteger counter = new AtomicInteger(0);
    CompilationPhaseListener listener = compilationEvent -> {
      var source = compilationEvent.source();
      var phase = compilationEvent.phase();
      var errorListener = source.getErrorListener();
      if (errorListener.hasErrors()) {
        reporter.report("Errors  : " + phase + ", source: " + source.getFileName());
        errorListener.getErrors().forEachRemaining(error -> {
          counter.getAndIncrement();
          reporter.report(error);
        });
        if (phase != upToPhase) {
          reporter.report("Had errors before reaching phase: " + upToPhase);
          Assertions.fail("Test result is not valid");
        }
      }

      if (source.getErrorListener().hasDirectiveErrors()) {
        reporter.report("Directiv: " + phase + ", source: " + source.getFileName());
        source.getErrorListener().getDirectiveErrors().forEachRemaining(error -> {
          counter.getAndIncrement();
          reporter.report(error);
        });
      }
      checkCompilationPhase(phase, source, sharedCompilableProgram);
    };

    FullPhaseSupplier allPhases = new FullPhaseSupplier(sharedCompilableProgram,
        listener, reporter);

    var compiler = new Ek9Compiler(allPhases, reporter.isMuteReportedErrors());
    sharedCompilableProgram.accept(this::assertPreConditions);

    var compilationResult = compiler.compile(ek9Workspace, new CompilerFlags(upToPhase, reporter.isVerbose()));

    sharedCompilableProgram.accept(program -> checkFinalResults(compilationResult, counter.get(), program));
  }
}

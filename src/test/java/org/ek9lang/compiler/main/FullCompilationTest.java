package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.ek9lang.compiler.errors.CompilationPhaseListener;
import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.internals.CompilableSource;
import org.ek9lang.compiler.internals.Workspace;
import org.ek9lang.compiler.main.phases.CompilationPhase;
import org.ek9lang.compiler.main.phases.options.FullPhaseSupplier;
import org.ek9lang.compiler.main.phases.result.CompilerReporter;
import org.ek9lang.compiler.parsing.WorkSpaceFromResourceDirectoryFiles;
import org.ek9lang.compiler.directives.DirectiveType;
import org.ek9lang.compiler.symbol.support.ShowAllSymbolsInAllModules;
import org.ek9lang.core.threads.SharedThreadContext;
import org.junit.jupiter.api.Assertions;

/**
 * Abstract base for the range of different tests that are needed.
 */
abstract class FullCompilationTest {

  private final Supplier<SharedThreadContext<CompilableProgram>> sharedContext = new CompilableProgramSuitable();

  private final Workspace ek9Workspace;

  private final ShowAllSymbolsInAllModules showAllSymbolsInAllModules = new ShowAllSymbolsInAllModules();

  public FullCompilationTest(final String fromResourcesDirectory) {
    var workspaceLoader = new WorkSpaceFromResourceDirectoryFiles();
    ek9Workspace = workspaceLoader.apply(fromResourcesDirectory);
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
    if(errorOnDirectiveErrors()) {
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
      if (source.getErrorListener().hasErrors()) {
        System.out.println("Errors  : " + phase + ", source: " + source.getFileName());
        source.getErrorListener().getErrors().forEachRemaining(error -> {
          counter.getAndIncrement();
          System.out.println(error);
        });
        if(phase != upToPhase) {
          System.out.println("Had errors before reaching phase: " + upToPhase);
          Assertions.fail("Test result is not valid");
        }
      }

      if (source.getErrorListener().hasDirectiveErrors()) {
        System.out.println("Directiv: " + phase + ", source: " + source.getFileName());
        source.getErrorListener().getDirectiveErrors().forEachRemaining(error -> {
          counter.getAndIncrement();
          System.out.println(error);
        });
      }
      checkCompilationPhase(phase, source, sharedCompilableProgram);
    };

    FullPhaseSupplier allPhases = new FullPhaseSupplier(sharedCompilableProgram,
        listener, new CompilerReporter(true));

    var compiler = new Ek9Compiler(allPhases);
    sharedCompilableProgram.accept(this::assertPreConditions);

    var compilationResult = compiler.compile(ek9Workspace, new CompilerFlags(upToPhase, true));

    sharedCompilableProgram.accept(program -> checkFinalResults(compilationResult, counter.get(), program));
  }
}

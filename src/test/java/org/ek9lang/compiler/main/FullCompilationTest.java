package org.ek9lang.compiler.main;

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
import org.ek9lang.core.threads.SharedThreadContext;

/**
 * Abstract base for the range of different tests that are needed.
 */
abstract class FullCompilationTest {

  private final Supplier<SharedThreadContext<CompilableProgram>> sharedContext = new CompilableProgramSuitable();

  private final Workspace ek9Workspace;

  public FullCompilationTest(final String fromResourcesDirectory) {
    var workspaceLoader = new WorkSpaceFromResourceDirectoryFiles();
    ek9Workspace = workspaceLoader.apply(fromResourcesDirectory);
  }

  /**
   * Optionally test classes can implement this for precondition processing.
   */
  protected void assertPreConditions(CompilableProgram program) {
  }

  protected abstract void assertFinalResults(final boolean compilationResult,
                                             final int numberOfErrors,
                                             final CompilableProgram program);

  /**
   * Override if the test needs to check any intermediate phase results.
   */
  protected void compilationPhaseCompleted(final CompilationPhase phase, final CompilableSource source,
                                           final SharedThreadContext<CompilableProgram> sharedCompilableProgram) {
  }

  protected void testToPhase(final CompilationPhase upToPhase) {
    //Just start with the basics and most on to the next phase one implemented.
    var sharedCompilableProgram = sharedContext.get();

    AtomicInteger counter = new AtomicInteger(0);
    CompilationPhaseListener listener = (phase, source) -> {
      if (!source.getErrorListener().isErrorFree()) {

        System.out.println("Errors  : " + phase + ", source: " + source.getFileName());
        source.getErrorListener().getErrors().forEachRemaining(error -> {
          counter.getAndIncrement();
          System.out.println(error);
        });
      }
      compilationPhaseCompleted(phase, source, sharedCompilableProgram);
    };

    FullPhaseSupplier allPhases = new FullPhaseSupplier(sharedCompilableProgram,
        listener, new CompilerReporter(true));

    var compiler = new Ek9Compiler(allPhases);
    sharedCompilableProgram.accept(program -> assertPreConditions(program));

    var compilationResult = compiler.compile(ek9Workspace, new CompilerFlags(upToPhase, true));

    sharedCompilableProgram.accept(program -> assertFinalResults(compilationResult, counter.get(), program));
  }
}

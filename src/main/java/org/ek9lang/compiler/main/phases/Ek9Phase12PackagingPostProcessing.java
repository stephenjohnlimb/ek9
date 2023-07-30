package org.ek9lang.compiler.main.phases;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import org.ek9lang.compiler.Workspace;
import org.ek9lang.compiler.errors.CompilationEvent;
import org.ek9lang.compiler.CompilerFlags;
import org.ek9lang.compiler.main.phases.result.CompilableSourceErrorCheck;
import org.ek9lang.compiler.main.phases.result.CompilationPhaseResult;
import org.ek9lang.compiler.main.phases.result.CompilerReporter;

/**
 * SINGLE THREADED
 * Complete any post-processing required. This could involve converting applications to
 * binary executable for example.
 */
public class Ek9Phase12PackagingPostProcessing implements
    BiFunction<Workspace, CompilerFlags, CompilationPhaseResult> {
  private static final CompilationPhase thisPhase = CompilationPhase.PACKAGING_POST_PROCESSING;
  private final Consumer<CompilationEvent> listener;
  private final CompilerReporter reporter;
  private final CompilableSourceErrorCheck sourceHaveErrors = new CompilableSourceErrorCheck();

  public Ek9Phase12PackagingPostProcessing(Consumer<CompilationEvent> listener,
                                           CompilerReporter reporter) {
    this.listener = listener;
    this.reporter = reporter;
  }

  @Override
  public CompilationPhaseResult apply(Workspace workspace, CompilerFlags compilerFlags) {

    return new CompilationPhaseResult(thisPhase, true,
        compilerFlags.getCompileToPhase() == thisPhase);
  }

}

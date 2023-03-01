package org.ek9lang.compiler.main.phases;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import org.ek9lang.compiler.errors.CompilationEvent;
import org.ek9lang.compiler.internals.Workspace;
import org.ek9lang.compiler.main.CompilerFlags;
import org.ek9lang.compiler.main.phases.result.CompilableSourceErrorCheck;
import org.ek9lang.compiler.main.phases.result.CompilationPhaseResult;
import org.ek9lang.compiler.main.phases.result.CompilerReporter;

/**
 * MULTI THREADED
 * Preparation to generate code. Now this may vary in implementation significantly depending on the
 * target architecture output.
 * See compilationContext.commandLine().targetArchitecture to determine what to prepare to create.
 */
public class Ek9Phase10CodeGenerationPreparation implements
    BiFunction<Workspace, CompilerFlags, CompilationPhaseResult> {
  private final Consumer<CompilationEvent> listener;
  private final CompilerReporter reporter;
  private final CompilableSourceErrorCheck sourceHaveErrors = new CompilableSourceErrorCheck();

  private static final CompilationPhase thisPhase = CompilationPhase.CODE_GENERATION_PREPARATION;

  public Ek9Phase10CodeGenerationPreparation(Consumer<CompilationEvent> listener,
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

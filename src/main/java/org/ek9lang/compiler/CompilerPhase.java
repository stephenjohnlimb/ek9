package org.ek9lang.compiler;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import org.ek9lang.compiler.common.CompilationEvent;
import org.ek9lang.compiler.common.CompilerReporter;
import org.ek9lang.core.SharedThreadContext;

/**
 * Marks the specific activity of a compiler phase.
 * Provides mechanism to pass both phase and the compiler flags into the compilable program.
 * This then enables them to be accessed within other components during compilation.
 */
public abstract class CompilerPhase implements BiFunction<Workspace, CompilerFlags, CompilationPhaseResult> {
  private final CompilationPhase thisPhase;
  protected final Consumer<CompilationEvent> listener;
  protected final CompilerReporter reporter;
  protected final SharedThreadContext<CompilableProgram> compilableProgramAccess;

  protected CompilerPhase(CompilationPhase phase, SharedThreadContext<CompilableProgram> compilableProgramAccess,
                          Consumer<CompilationEvent> listener, CompilerReporter reporter) {
    this.thisPhase = phase;
    this.listener = listener;
    this.reporter = reporter;
    this.compilableProgramAccess = compilableProgramAccess;
  }

  /**
   * Do the compilation phase.
   */
  protected abstract boolean doApply(Workspace workspace, CompilerFlags compilerFlags);

  @Override
  public CompilationPhaseResult apply(Workspace workspace, CompilerFlags compilerFlags) {
    enterPhase(compilableProgramAccess, reporter, new CompilationData(thisPhase, compilerFlags));
    final var result = doApply(workspace, compilerFlags);
    return new CompilationPhaseResult(thisPhase, result, compilerFlags.getCompileToPhase() == thisPhase);
  }

  private void enterPhase(SharedThreadContext<CompilableProgram> compilableProgramAccess,
                          final CompilerReporter reporter,
                          final CompilationData compilationData) {

    //First ensure compilable program is aware of current phase and flags.
    compilableProgramAccess.accept(program -> program.setCompilationData(compilationData));
    //Make a report that this phase has started.
    reporter.log(compilationData.phase());
  }
}

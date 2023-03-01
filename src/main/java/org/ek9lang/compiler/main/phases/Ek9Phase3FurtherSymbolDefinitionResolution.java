package org.ek9lang.compiler.main.phases;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import org.ek9lang.compiler.errors.CompilationEvent;
import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.internals.Workspace;
import org.ek9lang.compiler.main.CompilerFlags;
import org.ek9lang.compiler.main.phases.result.CompilableSourceErrorCheck;
import org.ek9lang.compiler.main.phases.result.CompilationPhaseResult;
import org.ek9lang.compiler.main.phases.result.CompilerReporter;
import org.ek9lang.core.threads.SharedThreadContext;

/**
 * MULTI THREADED
 * Now try and resolve the types of the symbols we have now defined. But still define additional
 * symbols as and when we can.
 * NOTE, this means that inference and assignments and the like mean we still won't know what the
 * type of some variables are at the end of this phase.
 * Only the simple case - clearly we'd need to do the initial template type defs and resolutions
 * before this phase that's why this is phase three not two! But phase 4 after this is where we
 * hydrate the template types to get real details on those types.
 */
public class Ek9Phase3FurtherSymbolDefinitionResolution
    implements BiFunction<Workspace, CompilerFlags, CompilationPhaseResult> {
  private final Consumer<CompilationEvent> listener;
  private final CompilerReporter reporter;
  private final SharedThreadContext<CompilableProgram> compilableProgramAccess;
  private final CompilableSourceErrorCheck sourceHaveErrors = new CompilableSourceErrorCheck();

  private static final CompilationPhase thisPhase = CompilationPhase.FURTHER_SYMBOL_DEFINITION;

  /**
   * Create new instance for further symbol resolution.
   */
  public Ek9Phase3FurtherSymbolDefinitionResolution(SharedThreadContext<CompilableProgram> compilableProgramAccess,
                                                    Consumer<CompilationEvent> listener, CompilerReporter reporter) {
    this.listener = listener;
    this.reporter = reporter;
    this.compilableProgramAccess = compilableProgramAccess;
  }

  @Override
  public CompilationPhaseResult apply(Workspace workspace, CompilerFlags compilerFlags) {
    return new CompilationPhaseResult(thisPhase, true, compilerFlags.getCompileToPhase() == thisPhase);
  }
}

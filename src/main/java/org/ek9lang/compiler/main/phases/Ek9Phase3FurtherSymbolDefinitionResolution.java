package org.ek9lang.compiler.main.phases;

import java.util.function.BiFunction;
import org.ek9lang.compiler.errors.CompilationListener;
import org.ek9lang.compiler.internals.Workspace;
import org.ek9lang.compiler.main.CompilerFlags;
import org.ek9lang.compiler.main.phases.result.CompilableSourceErrorCheck;
import org.ek9lang.compiler.main.phases.result.CompilationPhaseResult;
import org.ek9lang.compiler.main.phases.result.CompilerReporter;

/**
 * MULTI THREADED
 * Now try and resolve the types of the symbols we have now defined. But still define additional
 * symbols as and when we can.
 * NOTE, this means that inference and assignments and the like mean we still wont know what the
 * type of some variables are at the end of this phase.
 * Only the simple case - clearly we'd need to do the initial template type defs and resolutions
 * before this phase that's why this is phase three not two! But phase 4 after this is where we
 * hydrate the template types to get real details on those types.
 */
public class Ek9Phase3FurtherSymbolDefinitionResolution
    implements BiFunction<Workspace, CompilerFlags, CompilationPhaseResult> {
  private final CompilationListener listener;
  private final CompilerReporter reporter;
  private final CompilableSourceErrorCheck sourceHaveErrors = new CompilableSourceErrorCheck();

  public Ek9Phase3FurtherSymbolDefinitionResolution(CompilationListener listener, CompilerReporter reporter) {
    this.listener = listener;
    this.reporter = reporter;
  }

  @Override
  public CompilationPhaseResult apply(Workspace workspace, CompilerFlags compilerFlags) {
    final var thisPhase = CompilationPhase.FURTHER_SYMBOL_DEFINITION;
    return new CompilationPhaseResult(thisPhase, true, compilerFlags.getCompileToPhase() == thisPhase);
  }

}

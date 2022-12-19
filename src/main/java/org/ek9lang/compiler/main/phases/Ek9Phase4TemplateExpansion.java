package org.ek9lang.compiler.main.phases;

import java.util.function.BiFunction;
import org.ek9lang.compiler.errors.CompilationPhaseListener;
import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.internals.Workspace;
import org.ek9lang.compiler.main.CompilerFlags;
import org.ek9lang.compiler.main.phases.result.CompilableSourceErrorCheck;
import org.ek9lang.compiler.main.phases.result.CompilationPhaseResult;
import org.ek9lang.compiler.main.phases.result.CompilerReporter;
import org.ek9lang.core.threads.SharedThreadContext;

/**
 * SINGLE THREADED.
 * Need to hydrate the generic types and functions and then ensure all the parameters and return
 * types of the concrete type/function get populated.
 * So this is essential before we can go on to the meat and potatoes of resolving (phase 5).
 * We need to have real types and the like in place before we do that and this phase generates all
 * those real types from the generic/templates.
 * Now we also check 'text' components in here, because we need to ensure there is a pseudo type
 * for the text irrespective of language. But also we need to ensure that the pseudo type has all
 * the methods from all languages.
 * We can then check in validation if someone has included a method for "en" but not for "de"
 * for example.
 * i.e. we will validate that all language versions of a text item have all methods!
 * We have to do this in here because it is a single threaded parse, which means all the specific
 * language 'text' items have been defined. We can now extract methods and build that pseudo type
 * (as there is only one thread).
 */
public class Ek9Phase4TemplateExpansion implements
    BiFunction<Workspace, CompilerFlags, CompilationPhaseResult> {
  private final CompilationPhaseListener listener;
  private final CompilerReporter reporter;
  private final SharedThreadContext<CompilableProgram> compilableProgramAccess;
  private final CompilableSourceErrorCheck sourceHaveErrors = new CompilableSourceErrorCheck();

  /**
   * Create new instance for template expansion.
   */
  public Ek9Phase4TemplateExpansion(SharedThreadContext<CompilableProgram> compilableProgramAccess,
                                    CompilationPhaseListener listener, CompilerReporter reporter) {
    this.listener = listener;
    this.reporter = reporter;
    this.compilableProgramAccess = compilableProgramAccess;
  }

  @Override
  public CompilationPhaseResult apply(Workspace workspace, CompilerFlags compilerFlags) {
    final var thisPhase = CompilationPhase.TEMPLATE_EXPANSION;
    return new CompilationPhaseResult(thisPhase, true,
        compilerFlags.getCompileToPhase() == thisPhase);
  }
}

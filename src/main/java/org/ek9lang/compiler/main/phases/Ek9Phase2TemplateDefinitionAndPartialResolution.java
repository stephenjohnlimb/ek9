package org.ek9lang.compiler.main.phases;

import java.util.function.BiFunction;
import org.ek9lang.compiler.errors.CompilationListener;
import org.ek9lang.compiler.files.Workspace;
import org.ek9lang.compiler.main.CompilerFlags;
import org.ek9lang.compiler.main.phases.result.CompilableSourceErrorCheck;
import org.ek9lang.compiler.main.phases.result.CompilationPhaseResult;
import org.ek9lang.compiler.main.phases.result.CompilerReporter;

/**
 * SINGLE THREADED
 * We don't need too much by way of dealing with scopes for phase two.
 * But we do have to resolve generic types like U, V, T etc that have been defined as part of a
 * class. This has to be a phased approach - so while this is phases2 it has a conceptual pass
 * and a concrete pass.
 * Conceptual builds the list of dependencies within conceptual generic classes and functions.
 * The Concrete phase then goes through a recursively builds the concrete types.
 */
public class Ek9Phase2TemplateDefinitionAndPartialResolution implements
    BiFunction<Workspace, CompilerFlags, CompilationPhaseResult> {
  private final CompilationListener listener;
  private final CompilerReporter reporter;
  private final CompilableSourceErrorCheck sourceHaveErrors = new CompilableSourceErrorCheck();

  public Ek9Phase2TemplateDefinitionAndPartialResolution(CompilationListener listener,
                                                         CompilerReporter reporter) {
    this.listener = listener;
    this.reporter = reporter;
  }

  @Override
  public CompilationPhaseResult apply(Workspace workspace, CompilerFlags compilerFlags) {
    final var thisPhase = CompilationPhase.TEMPLATE_DEFINITION_RESOLUTION;
    return new CompilationPhaseResult(thisPhase, true,
        compilerFlags.getCompileToPhase() == thisPhase);
  }
}

package org.ek9lang.compiler.phase5;

import java.util.function.Consumer;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilableSource;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.CompilerFlags;
import org.ek9lang.compiler.CompilerPhase;
import org.ek9lang.compiler.Workspace;
import org.ek9lang.compiler.common.CompilableSourceHasErrors;
import org.ek9lang.compiler.common.CompilationEvent;
import org.ek9lang.compiler.common.CompilerReporter;
import org.ek9lang.core.SharedThreadContext;

/**
 * At this point all templates should be present and all symbols resolved.
 * This check phase is to look more at the flow of the code and see if there are potential
 * errors that could be avoided at compile time, rather than allowing some form of runtime failure.
 */
public class PreIntermediateRepresentationChecks extends CompilerPhase {
  private static final CompilationPhase thisPhase = CompilationPhase.PRE_IR_CHECKS;
  private final CompilableSourceHasErrors sourceHasErrors = new CompilableSourceHasErrors();

  public PreIntermediateRepresentationChecks(final SharedThreadContext<CompilableProgram> compilableProgramAccess,
                                             final Consumer<CompilationEvent> listener,
                                             final CompilerReporter reporter) {

    super(thisPhase, compilableProgramAccess, listener, reporter);

  }

  @Override
  protected boolean doApply(final Workspace workspace, final CompilerFlags compilerFlags) {

    workspace.getSources()
        .parallelStream()
        .forEach(this::structureValidOrError);

    return !sourceHasErrors.test(workspace.getSources());
  }

  private void structureValidOrError(final CompilableSource source) {

    final var parsedModule = getParsedModuleForSource(source);
    final var phaseListener = new PreIRListener(parsedModule);
    final var walker = new ParseTreeWalker();

    walker.walk(phaseListener, source.getCompilationUnitContext());
    listener.accept(new CompilationEvent(thisPhase, parsedModule, source));
  }

}

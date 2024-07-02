package org.ek9lang.compiler.phase5;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilableSource;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.CompilerFlags;
import org.ek9lang.compiler.CompilerPhase;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.compiler.Workspace;
import org.ek9lang.compiler.common.CompilableSourceHasErrors;
import org.ek9lang.compiler.common.CompilationEvent;
import org.ek9lang.compiler.common.CompilerReporter;
import org.ek9lang.core.CompilerException;
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
        .forEach(this::resolveOrDefineTypeSymbols);

    return !sourceHasErrors.test(workspace.getSources());
  }

  private void resolveOrDefineTypeSymbols(final CompilableSource source) {

    //First get the parsed module for this source file.
    //This has to be done via a mutable holder through a reentrant lock to the program
    final var holder = new AtomicReference<ParsedModule>();
    compilableProgramAccess.accept(
        program -> holder.set(program.getParsedModuleForCompilableSource(source))
    );

    if (holder.get() == null) {
      throw new CompilerException("Compiler error, the parsed module must be present for " + source.getFileName());
    } else {
      final var parsedModule = holder.get();
      final var phaseListener = new PreIRCheckListener(parsedModule);
      final var walker = new ParseTreeWalker();

      walker.walk(phaseListener, source.getCompilationUnitContext());
      listener.accept(new CompilationEvent(thisPhase, parsedModule, source));
    }
  }
}

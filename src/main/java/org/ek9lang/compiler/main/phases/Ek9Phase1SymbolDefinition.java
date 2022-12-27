package org.ek9lang.compiler.main.phases;

import java.util.function.BiFunction;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.ek9lang.compiler.errors.CompilationPhaseListener;
import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.internals.CompilableSource;
import org.ek9lang.compiler.internals.ParsedModule;
import org.ek9lang.compiler.internals.Workspace;
import org.ek9lang.compiler.main.CompilerFlags;
import org.ek9lang.compiler.main.phases.definition.DefinitionPhase1Listener;
import org.ek9lang.compiler.main.phases.result.CompilableSourceErrorCheck;
import org.ek9lang.compiler.main.phases.result.CompilationPhaseResult;
import org.ek9lang.compiler.main.phases.result.CompilerReporter;
import org.ek9lang.core.threads.SharedThreadContext;

/**
 * Can be MULTI THREADED for developer source, but single threaded for bootstrapping.
 * Goes through the now successfully parse source files and uses
 * a visitor to do the first real pass at building the IR - simple Symbol definitions.
 * This means identifying types and other symbols.
 */
public class Ek9Phase1SymbolDefinition implements BiFunction<Workspace, CompilerFlags, CompilationPhaseResult> {

  private boolean useMultiThreading = true;
  private final CompilationPhaseListener listener;
  private final CompilerReporter reporter;
  private final SharedThreadContext<CompilableProgram> compilableProgramAccess;
  private final CompilableSourceErrorCheck sourceHaveErrors = new CompilableSourceErrorCheck();

  /**
   * Create a new phase 1 symbol definition instance.
   */
  public Ek9Phase1SymbolDefinition(SharedThreadContext<CompilableProgram> compilableProgramAccess,
                                   CompilationPhaseListener listener, CompilerReporter reporter) {
    this.listener = listener;
    this.reporter = reporter;
    this.compilableProgramAccess = compilableProgramAccess;
  }

  public Ek9Phase1SymbolDefinition(boolean multiThread, SharedThreadContext<CompilableProgram> compilableProgramAccess,
                                   CompilationPhaseListener listener, CompilerReporter reporter) {
    this(compilableProgramAccess, listener, reporter);
    this.useMultiThreading = multiThread;
  }

  @Override
  public CompilationPhaseResult apply(Workspace workspace, CompilerFlags compilerFlags) {
    final var thisPhase = CompilationPhase.SYMBOL_DEFINITION;
    final var result = underTakeSymbolDefinition(workspace, thisPhase);
    reporter.log(thisPhase);
    return new CompilationPhaseResult(thisPhase, result, compilerFlags.getCompileToPhase() == thisPhase);
  }

  private boolean underTakeSymbolDefinition(Workspace workspace, CompilationPhase phase) {
    //May consider moving to Executor model

    if (useMultiThreading) {
      defineSymbolsMultiThreaded(workspace);
    } else {
      defineSymbolsSingleThreaded(workspace);
    }

    workspace.getSources().forEach(source -> listener.accept(phase, source));
    return !sourceHaveErrors.test(workspace.getSources());
  }

  private void defineSymbolsMultiThreaded(Workspace workspace) {
    //May consider moving to Executor model
    workspace.getSources()
        .stream()
        .forEach(this::defineSymbols);
  }

  private void defineSymbolsSingleThreaded(Workspace workspace) {
    workspace.getSources()
        .stream()
        .forEach(this::defineSymbols);
  }

  /**
   * THIS IS WHERE THE DEFINITION PHASE 1 LISTENER IS CREATED AND USED.
   * This must create 'new' stuff; except for compilableProgramAccess.
   */
  private void defineSymbols(CompilableSource source) {
    final ParsedModule module = new ParsedModule(source, compilableProgramAccess);
    module.acceptCompilationUnitContext(source.getCompilationUnitContext());

    DefinitionPhase1Listener phaseListener = new DefinitionPhase1Listener(compilableProgramAccess, module);
    ParseTreeWalker walker = new ParseTreeWalker();
    walker.walk(phaseListener, source.getCompilationUnitContext());
    if (source.getErrorListener().isErrorFree()) {
      compilableProgramAccess.accept(compilableProgram -> compilableProgram.add(module));
    }
  }
}

package org.ek9lang.compiler.phase1;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilableSource;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.CompilerFlags;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.compiler.Workspace;
import org.ek9lang.compiler.errors.CompilationEvent;
import org.ek9lang.compiler.common.CompilableSourceErrorCheck;
import org.ek9lang.compiler.common.CompilationPhaseResult;
import org.ek9lang.compiler.common.CompilerReporter;
import org.ek9lang.core.SharedThreadContext;

/**
 * Can be MULTI THREADED for developer source, but single threaded for bootstrapping.
 * Goes through the now successfully parse source files and uses
 * a visitor to do the first real pass at building the IR - simple Symbol definitions.
 * This means identifying types and other symbols.
 * WILL WAIT FOR Java 21 with full virtual Threads.
 */
public final class Ek9Phase1SymbolDefinition implements BiFunction<Workspace, CompilerFlags, CompilationPhaseResult> {

  private static final CompilationPhase thisPhase = CompilationPhase.SYMBOL_DEFINITION;
  private final Consumer<CompilationEvent> listener;
  private final CompilerReporter reporter;
  private final SharedThreadContext<CompilableProgram> compilableProgramAccess;
  private final CompilableSourceErrorCheck sourceHaveErrors = new CompilableSourceErrorCheck();
  private boolean useMultiThreading = true;

  /**
   * Create a new phase 1 symbol definition instance.
   */
  public Ek9Phase1SymbolDefinition(SharedThreadContext<CompilableProgram> compilableProgramAccess,
                                   Consumer<CompilationEvent> listener, CompilerReporter reporter) {
    this.listener = listener;
    this.reporter = reporter;
    this.compilableProgramAccess = compilableProgramAccess;
  }

  public Ek9Phase1SymbolDefinition(boolean multiThread, SharedThreadContext<CompilableProgram> compilableProgramAccess,
                                   Consumer<CompilationEvent> listener, CompilerReporter reporter) {
    this(compilableProgramAccess, listener, reporter);
    this.useMultiThreading = multiThread;
  }

  @Override
  public CompilationPhaseResult apply(Workspace workspace, CompilerFlags compilerFlags) {

    reporter.log(thisPhase);
    final var result = underTakeSymbolDefinition(workspace);
    return new CompilationPhaseResult(thisPhase, result, compilerFlags.getCompileToPhase() == thisPhase);
  }

  private boolean underTakeSymbolDefinition(Workspace workspace) {
    //May consider moving to Executor model

    if (useMultiThreading) {
      defineSymbolsMultiThreaded(workspace);
    } else {
      defineSymbolsSingleThreaded(workspace);
    }

    return !sourceHaveErrors.test(workspace.getSources());
  }

  private void defineSymbolsMultiThreaded(Workspace workspace) {
    workspace.getSources()
        .parallelStream()
        .forEach(this::defineSymbols);
  }

  private void defineSymbolsSingleThreaded(Workspace workspace) {
    workspace.getSources().forEach(this::defineSymbols);
  }

  /**
   * THIS IS WHERE THE DEFINITION PHASE 1 LISTENER IS CREATED AND USED.
   * This must create 'new' stuff; except for compilableProgramAccess.
   */
  private void defineSymbols(CompilableSource source) {
    final ParsedModule parsedModule = new ParsedModule(source, compilableProgramAccess);
    parsedModule.acceptCompilationUnitContext(source.getCompilationUnitContext());
    //Need to add this early - even though there may be compiler errors
    //Otherwise several files in same module will not detect duplicate symbols.
    compilableProgramAccess.accept(compilableProgram -> compilableProgram.add(parsedModule));

    DefinitionPhase1Listener phaseListener = new DefinitionPhase1Listener(parsedModule);
    ParseTreeWalker walker = new ParseTreeWalker();
    walker.walk(phaseListener, source.getCompilationUnitContext());
    listener.accept(new CompilationEvent(thisPhase, parsedModule, source));

    /*
     * Now for the built-in types, we resolve and hold the types and supply to the compilable program.
     * These can then be passed into ParsedModules as and when requested and then into other components.
     */
    if ("org-ek9-lang.ek9".equals(source.getFileName())) {
      final var builtInTypeCacheResolver = new BuiltInTypeCacheResolver();
      final var ek9Types = builtInTypeCacheResolver.apply(parsedModule.getModuleScope());
      compilableProgramAccess.accept(compilableProgram -> compilableProgram.setEk9Types(ek9Types));
    }
  }
}

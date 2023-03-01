package org.ek9lang.compiler.main.phases;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.ek9lang.compiler.errors.CompilationEvent;
import org.ek9lang.compiler.errors.CompilationPhaseListener;
import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.internals.CompilableSource;
import org.ek9lang.compiler.internals.ParsedModule;
import org.ek9lang.compiler.internals.Workspace;
import org.ek9lang.compiler.main.CompilerFlags;
import org.ek9lang.compiler.main.phases.definition.ResolveDefineExplicitTemplateUseListener;
import org.ek9lang.compiler.main.phases.result.CompilableSourceErrorCheck;
import org.ek9lang.compiler.main.phases.result.CompilationPhaseResult;
import org.ek9lang.compiler.main.phases.result.CompilerReporter;
import org.ek9lang.core.exception.CompilerException;
import org.ek9lang.core.threads.SharedThreadContext;
import org.ek9lang.core.utils.Holder;

/**
 * Can be MULTI THREADED for developer source, but single threaded for bootstrapping.
 * Goes through the now successfully parse source files and uses
 * a visitor to do the second real pass at building the IR - more explicit type symbol definitions.
 * The first pass 'Ek9Phase1SymbolDefinition' will have defined many symbols, types, functions.
 * But the order may well have resulted in some explicit uses of generic types not being fully resolved
 * in terms of the parameterization used. i.e. 'UseOfBuiltInGenerics4.ek9' shows a class being declared after it has
 * been used in conjunctions with a generic/template type.
 * So this phase is designed to complete a second pass - but this time as all explicit types should be known
 * (non-inferred use only). The resolver will be configured to emit errors when explicit polymorphic parameterization
 * fails.
 * NOTE, we are not trying to do the inferred 'polymorphic parameterization' like 'list <- List(2)'.
 * We're only looking to do 'list as List of Integer' and list <- List() of Integer sorts of checks, but with
 * developer created Template types and classes - which can appear in any order and in any file/module.
 * Hence, the need for a second pass.
 */
public class Ek9Phase1NonInferredTypeDefinition
    implements BiFunction<Workspace, CompilerFlags, CompilationPhaseResult> {

  private boolean useMultiThreading = true;
  private final Consumer<CompilationEvent> listener;
  private final CompilerReporter reporter;
  private final SharedThreadContext<CompilableProgram> compilableProgramAccess;
  private final CompilableSourceErrorCheck sourceHaveErrors = new CompilableSourceErrorCheck();

  private static final CompilationPhase thisPhase = CompilationPhase.EXPLICIT_TYPE_SYMBOL_DEFINITION;

  /**
   * Create a new phase 1 second pass template type symbol resolution definition instance.
   */
  public Ek9Phase1NonInferredTypeDefinition(SharedThreadContext<CompilableProgram> compilableProgramAccess,
                                            Consumer<CompilationEvent> listener, CompilerReporter reporter) {
    this.listener = listener;
    this.reporter = reporter;
    this.compilableProgramAccess = compilableProgramAccess;
  }

  @Override
  public CompilationPhaseResult apply(Workspace workspace, CompilerFlags compilerFlags) {


    reporter.log(thisPhase);
    final var result = underTakeTypeSymbolResolutionAndDefinition(workspace);
    return new CompilationPhaseResult(thisPhase, result, compilerFlags.getCompileToPhase() == thisPhase);
  }

  private boolean underTakeTypeSymbolResolutionAndDefinition(Workspace workspace) {
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
        .forEach(this::resolveOrDefineTypeSymbols);
  }

  private void defineSymbolsSingleThreaded(Workspace workspace) {
    workspace.getSources().forEach(this::resolveOrDefineTypeSymbols);
  }

  /**
   * THIS IS WHERE THE EXPLICIT TEMPLATE TYPE USE PHASE 1 LISTENER IS CREATED AND USED.
   */
  private void resolveOrDefineTypeSymbols(CompilableSource source) {
    //First get the parsed module for this source file.
    //This has to be done via a mutable holder through a reentrant lock to the program
    var holder = new Holder<ParsedModule>();
    compilableProgramAccess.accept(
        program -> holder.accept(Optional.ofNullable(program.getParsedModuleForCompilableSource(source))));

    if (holder.isEmpty()) {
      throw new CompilerException("Compiler error, the parsed module must be present for " + source.getFileName());
    }

    holder.ifPresent(parsedModule -> {
      ResolveDefineExplicitTemplateUseListener phaseListener =
          new ResolveDefineExplicitTemplateUseListener(parsedModule);
      ParseTreeWalker walker = new ParseTreeWalker();
      walker.walk(phaseListener, source.getCompilationUnitContext());
      listener.accept(new CompilationEvent(thisPhase, parsedModule, source));
    });
  }
}

package org.ek9lang.compiler.phase2;

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
import org.ek9lang.compiler.common.CompilableSourceErrorCheck;
import org.ek9lang.compiler.common.CompilationEvent;
import org.ek9lang.compiler.common.CompilerReporter;
import org.ek9lang.core.SharedThreadContext;

/**
 * Can be MULTI THREADED for developer source, but single threaded for bootstrapping.
 * <p>
 * Goes through the now successfully parsed source files and uses
 * a listener to do the second real pass at building the symbols - more explicit type symbol definitions.
 * </p>
 * <p>
 * The first pass 'SymbolDefinition' will have defined many symbols, types, functions.
 * But the order may well have resulted in some explicit uses of generic types not being fully resolved
 * in terms of the parameterization used. i.e. 'UseOfBuiltInGenerics4.ek9' shows a class being declared after it has
 * been used in conjunctions with a generic/template type.
 * </p>
 * <p>
 * So this phase is designed to complete a second pass - but this time as all explicit types should be known
 * (non-inferred use only). The resolver will be configured to emit errors when explicit polymorphic parameterization
 * fails.
 * </p>
 * <p>
 * NOTE, we are not trying to do the inferred 'polymorphic parameterization' like 'list &larr; List(2)'.
 * We're only looking to do 'list as List of Integer' and list &larr; List() of Integer sorts of checks, but with
 * developer created Template types and classes - which can appear in any order and in any file/module.
 * Hence, the need for a second pass. We also need to start building type hierarchies as these are important for
 * generic types and parameterised type use.
 * </p>
 */
public final class NonInferredTypeDefinition extends CompilerPhase {

  private static final CompilationPhase thisPhase = CompilationPhase.EXPLICIT_TYPE_SYMBOL_DEFINITION;
  private final boolean useMultiThreading;

  private final CompilableSourceErrorCheck sourceHaveErrors = new CompilableSourceErrorCheck();

  /**
   * Create a new phase 1 second pass template type symbol resolution definition instance.
   */
  public NonInferredTypeDefinition(final boolean multiThread,
                                   SharedThreadContext<CompilableProgram> compilableProgramAccess,
                                   Consumer<CompilationEvent> listener, CompilerReporter reporter) {
    super(thisPhase, compilableProgramAccess, listener, reporter);
    this.useMultiThreading = multiThread;
  }

  @Override
  public boolean doApply(Workspace workspace, CompilerFlags compilerFlags) {
    return underTakeTypeSymbolResolutionAndDefinition(workspace);
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
    var holder = new AtomicReference<ParsedModule>();
    //Only hold the lock for the minimal time.
    compilableProgramAccess.accept(
        program -> holder.set(program.getParsedModuleForCompilableSource(source)));

    var parsedModule = holder.get();
    if (parsedModule != null) {
      ResolveDefineExplicitTypeListener phaseListener =
          new ResolveDefineExplicitTypeListener(parsedModule);
      ParseTreeWalker walker = new ParseTreeWalker();
      walker.walk(phaseListener, source.getCompilationUnitContext());
      listener.accept(new CompilationEvent(thisPhase, parsedModule, source));
    }
  }
}

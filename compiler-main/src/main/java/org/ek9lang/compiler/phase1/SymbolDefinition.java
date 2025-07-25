package org.ek9lang.compiler.phase1;

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
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.support.AggregateManipulator;
import org.ek9lang.compiler.tokenizer.Ek9Token;
import org.ek9lang.core.SharedThreadContext;

/**
 * Can be MULTI THREADED for developer source, but must be single threaded for bootstrapping.
 * Goes through the now successfully parse source files and uses
 * a listener to do the first real pass at building the IR - simple Symbol definitions.
 * This means identifying types and other symbols.
 * Uses Java 21 with full virtual Threads.
 * Note that most of the real processing is done in the
 * {@link org.ek9lang.compiler.phase1.DefinitionListener}.
 * This is just the wrapper entry point that is public, so it can be plugged into the
 * {@link org.ek9lang.compiler.config.FrontEndSupplier}.
 */
public final class SymbolDefinition extends CompilerPhase {

  private static final CompilationPhase thisPhase = CompilationPhase.SYMBOL_DEFINITION;
  private final CompilableSourceHasErrors sourceHasErrors = new CompilableSourceHasErrors();
  private boolean notBootStrapping = true;

  /**
   * Create a new phase 1 symbol definition instance, defaults to multithreading enabled.
   */
  public SymbolDefinition(final SharedThreadContext<CompilableProgram> compilableProgramAccess,
                          final Consumer<CompilationEvent> listener,
                          final CompilerReporter reporter) {

    super(thisPhase, compilableProgramAccess, listener, reporter);

  }

  /**
   * Create symbol definition instance with optional multi-threading.
   */
  public SymbolDefinition(final boolean notBootStrapping,
                          final SharedThreadContext<CompilableProgram> compilableProgramAccess,
                          final Consumer<CompilationEvent> listener,
                          final CompilerReporter reporter) {

    this(compilableProgramAccess, listener, reporter);
    this.notBootStrapping = notBootStrapping;

  }

  @Override
  public boolean doApply(final Workspace workspace, final CompilerFlags compilerFlags) {

    return underTakeSymbolDefinition(workspace);
  }

  private boolean underTakeSymbolDefinition(final Workspace workspace) {
    //May consider moving to Executor model

    if (notBootStrapping) {
      defineSymbolsMultiThreaded(workspace);
    } else {
      defineSymbolsSingleThreaded(workspace);
    }

    return !sourceHasErrors.test(workspace.getSources());
  }

  private void defineSymbolsMultiThreaded(final Workspace workspace) {

    workspace.getSources()
        .parallelStream()
        .forEach(this::defineSymbols);

  }

  private void defineSymbolsSingleThreaded(final Workspace workspace) {

    workspace.getSources().forEach(this::defineSymbols);

  }

  /**
   * THIS IS WHERE THE DEFINITION PHASE 1 LISTENER IS CREATED AND USED.
   * This must create 'new' stuff; except for compilableProgramAccess.
   */
  private void defineSymbols(final CompilableSource source) {

    final ParsedModule parsedModule = new ParsedModule(source, compilableProgramAccess);
    parsedModule.acceptCompilationUnitContext(source.getCompilationUnitContext());

    //Need to add this early - even though there may be compiler errors
    //Otherwise several files in same module will not detect duplicate symbols.
    compilableProgramAccess.accept(compilableProgram -> compilableProgram.add(parsedModule));

    final var walker = new ParseTreeWalker();
    walker.walk(new DefinitionListener(parsedModule), source.getCompilationUnitContext());

    //Do not allow use of the EK9 module name-spaces outside of boot-strapping the compiler.
    if (notBootStrapping
        && (AggregateManipulator.EK9_LANG.equals(parsedModule.getModuleName())
        || AggregateManipulator.EK9_MATH.equals(parsedModule.getModuleName()))) {

      source.getErrorListener().semanticError(new Ek9Token(parsedModule.getModuleName()),
          "", ErrorListener.SemanticClassification.INVALID_MODULE_NAME);

    }
    //Now inform the listener that this phase has been completed for this source file
    listener.accept(new CompilationEvent(thisPhase, parsedModule, source));

    //If not boot-strapping then do not record types from a module with same name
    if (notBootStrapping) {
      return;
    }

    /*
     * Now for the built-in types, we resolve and hold the types and supply to the compilable program.
     * These can then be passed into Modules as and when requested and then into other components.
     */
    if (AggregateManipulator.EK9_LANG.equals(parsedModule.getModuleName())) {
      final var builtInTypeCacheResolver = new BuiltInTypeCacheResolver();
      final var ek9Types = builtInTypeCacheResolver.apply(parsedModule.getModuleScope());
      compilableProgramAccess.accept(compilableProgram -> compilableProgram.setEk9Types(ek9Types));
    }
  }

}

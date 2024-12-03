package org.ek9lang.compiler.phase4;

import java.util.function.Consumer;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.CompilerFlags;
import org.ek9lang.compiler.CompilerPhase;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.compiler.Workspace;
import org.ek9lang.compiler.common.CompilableSourceHasErrors;
import org.ek9lang.compiler.common.CompilationEvent;
import org.ek9lang.compiler.common.CompilerReporter;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.PossibleGenericSymbol;
import org.ek9lang.core.SharedThreadContext;

/**
 * SINGLE THREADED
 * Once full resolution has completed, additional checks need to be made.
 * These relate to Generic Types, are assumed operators present on the type arguments.
 * Are the types used when subtyping constrained generic types appropriate.
 * But there could be several other post resolution checks if required.
 * Ideally most checks will have been done as early as possible, but as EK9 is quite
 * dynamic and has inference it means that not all checks can be completed until now.
 * It's a bit of brain fuzzer - because it relates to generics and type of types.
 * This class just deals with traversing the compilable program and all the modules,
 * then it calls the ParameterisedTypeOrError to check each in turn.
 */
public class PostSymbolResolutionChecks extends CompilerPhase {
  private static final CompilationPhase thisPhase = CompilationPhase.POST_RESOLUTION_CHECKS;
  private final CompilableSourceHasErrors sourceHasErrors = new CompilableSourceHasErrors();

  /**
   * Create new instance to check everything is logical and cohesive.
   */
  public PostSymbolResolutionChecks(final SharedThreadContext<CompilableProgram> compilableProgramAccess,
                                    final Consumer<CompilationEvent> listener,
                                    final CompilerReporter reporter) {

    super(thisPhase, compilableProgramAccess, listener, reporter);

  }

  @Override
  public boolean doApply(final Workspace workspace, final CompilerFlags compilerFlags) {

    parameterisedTypesValidOrError();

    return !sourceHasErrors.test(workspace.getSources());
  }

  private void parameterisedTypesValidOrError() {

    compilableProgramAccess.accept(program -> {
      for (var moduleName : program.getParsedModuleNames()) {
        final var parsedModules = program.getParsedModules(moduleName);

        for (var parsedModule : parsedModules) {
          parameterisedTypesValidInModuleOrError(parsedModule);
          listener.accept(new CompilationEvent(thisPhase, parsedModule, parsedModule.getSource()));
        }
      }
    });

  }

  private void parameterisedTypesValidInModuleOrError(final ParsedModule parsedModule) {

    final var errorListener = parsedModule.getSource().getErrorListener();
    final ParameterisedTypeOrError consumer = new ParameterisedTypeOrError(parsedModule.getEk9Types(), errorListener);
    final var scope = parsedModule.getModuleScope();

    scope.getSymbolsForThisScope().stream()
        .filter(ISymbol::isParameterisedType)
        .map(PossibleGenericSymbol.class::cast)
        .forEach(consumer);
  }

}

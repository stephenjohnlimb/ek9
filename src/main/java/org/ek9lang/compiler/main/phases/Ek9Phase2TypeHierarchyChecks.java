package org.ek9lang.compiler.main.phases;

import java.util.HashMap;
import java.util.HashSet;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.Workspace;
import org.ek9lang.compiler.errors.CompilationEvent;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.CompilerFlags;
import org.ek9lang.compiler.main.phases.result.CompilableSourceErrorCheck;
import org.ek9lang.compiler.main.phases.result.CompilationPhaseResult;
import org.ek9lang.compiler.main.phases.result.CompilerReporter;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.AggregateWithTraitsSymbol;
import org.ek9lang.compiler.symbols.FunctionSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.core.SharedThreadContext;

/**
 * SINGLE THREADED - Run across sources to check for types, functions and traits for 'super loops'.
 */
public class Ek9Phase2TypeHierarchyChecks implements BiFunction<Workspace, CompilerFlags, CompilationPhaseResult> {
  private static final CompilationPhase thisPhase = CompilationPhase.TYPE_HIERARCHY_CHECKS;
  private final Consumer<CompilationEvent> listener;
  private final CompilerReporter reporter;
  private final SharedThreadContext<CompilableProgram> compilableProgramAccess;
  private final CompilableSourceErrorCheck sourceHaveErrors = new CompilableSourceErrorCheck();

  /**
   * Create a new instance for checking of type hierarchies.
   */
  public Ek9Phase2TypeHierarchyChecks(SharedThreadContext<CompilableProgram> compilableProgramAccess,
                                      Consumer<CompilationEvent> listener, CompilerReporter reporter) {
    this.listener = listener;
    this.reporter = reporter;
    this.compilableProgramAccess = compilableProgramAccess;
  }

  @Override
  public CompilationPhaseResult apply(Workspace workspace, CompilerFlags compilerFlags) {
    reporter.log(thisPhase);
    checkForCircularHierarchies();

    var errorFree = !sourceHaveErrors.test(workspace.getSources());

    return new CompilationPhaseResult(thisPhase, errorFree,
        compilerFlags.getCompileToPhase() == thisPhase);


  }

  private void checkForCircularHierarchies() {
    compilableProgramAccess.accept(program -> {

      //Memoization, only check if not already checked (else On2 or worse).
      HashMap<String, ISymbol> processedSymbols = new HashMap<>();

      for (var moduleName : program.getParsedModuleNames()) {
        var parsedModules = program.getParsedModules(moduleName);

        for (var parsedModule : parsedModules) {
          var scope = parsedModule.getModuleScope();
          for (var symbol : scope.getSymbolsForThisScope()) {
            var errorListener = parsedModule.getSource().getErrorListener();
            checkForCircularLoops(errorListener, processedSymbols, new HashSet<>(), symbol);
          }
          listener.accept(new CompilationEvent(thisPhase, parsedModule, parsedModule.getSource()));
        }
      }
    });
  }

  private void checkForCircularLoops(ErrorListener errorListener,
                                     HashMap<String, ISymbol> processedSymbols,
                                     HashSet<String> symbolsEncountered,
                                     final ISymbol maybeHasASuper) {
    var fullyQualifiedName = maybeHasASuper.getFullyQualifiedName();

    if (symbolsEncountered.contains(fullyQualifiedName)) {
      //Well looks like within this hierarchy we've already encountered this symbol.
      var msg = "with genus '" + maybeHasASuper.getGenus().getDescription() + "' is invalid:";
      errorListener.semanticError(maybeHasASuper.getSourceToken(), msg,
          ErrorListener.SemanticClassification.CIRCULAR_HIERARCHY_DETECTED);
    }

    //Ok, so not encountered within this hierarchy, but has it already been processed?
    if (processedSymbols.containsKey(fullyQualifiedName)) {
      return; //already checked so stop now
    }

    //So marked that we've seen this symbol in general terms and
    //Also now we've seen it withing this hierarchy.
    processedSymbols.put(fullyQualifiedName, maybeHasASuper);
    symbolsEncountered.add(fullyQualifiedName);

    if (maybeHasASuper instanceof AggregateWithTraitsSymbol aggregateWithTraits) {
      aggregateWithTraits.getTraits()
          .forEach(trait -> checkForCircularLoops(errorListener, processedSymbols, symbolsEncountered, trait));
    }

    if (maybeHasASuper instanceof AggregateSymbol aggregate
        && aggregate.getSuperAggregateSymbol().isPresent()) {
      checkForCircularLoops(errorListener, processedSymbols, symbolsEncountered,
          aggregate.getSuperAggregateSymbol().get());
    } else if (maybeHasASuper instanceof FunctionSymbol function
        && function.getSuperFunctionSymbol().isPresent()) {
      checkForCircularLoops(errorListener, processedSymbols, symbolsEncountered,
          function.getSuperFunctionSymbol().get());
    }
    //else it cannot have a super. So that's it.
  }
}
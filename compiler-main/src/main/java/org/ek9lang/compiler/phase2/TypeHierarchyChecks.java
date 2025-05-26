package org.ek9lang.compiler.phase2;

import java.util.HashMap;
import java.util.HashSet;
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
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.AggregateWithTraitsSymbol;
import org.ek9lang.compiler.symbols.FunctionSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.SymbolGenus;
import org.ek9lang.core.SharedThreadContext;

/**
 * SINGLE THREADED - Run across sources to check for types, functions and traits for 'super loops'.
 * <p>
 * This is a bit nasty, basically there is a need to follow supers all the way back.
 * For aggregates this is just 'getSuperAggregateSymbol' for functions 'getSuperFunctionSymbol'.
 * </p>
 * <p>
 * But for traits - you need to get all its traits.
 * But note that it is important to flip between an aggregate hierarchy and a trait one.
 * </p>
 * <p>
 * TODO consider checking property loops, not when type typed but when loops in constructor calls.
 * </p>
 */
public final class TypeHierarchyChecks extends CompilerPhase {
  private static final CompilationPhase thisPhase = CompilationPhase.TYPE_HIERARCHY_CHECKS;
  private final CompilableSourceHasErrors sourceHasErrors = new CompilableSourceHasErrors();

  /**
   * Create a new instance for checking of type hierarchies.
   */
  public TypeHierarchyChecks(final SharedThreadContext<CompilableProgram> compilableProgramAccess,
                             final Consumer<CompilationEvent> listener,
                             final CompilerReporter reporter) {

    super(thisPhase, compilableProgramAccess, listener, reporter);

  }

  @Override
  public boolean doApply(final Workspace workspace, final CompilerFlags compilerFlags) {

    compilableProgramAccess.accept(this::checkHierarchies);

    return !sourceHasErrors.test(workspace.getSources());
  }

  private void checkHierarchies(final CompilableProgram program) {

    //Memoization, only check if not already checked (else On2 or worse).
    final HashMap<String, ISymbol> processedSymbols = new HashMap<>();

    for (var moduleName : program.getParsedModuleNames()) {
      final var parsedModules = program.getParsedModules(moduleName);

      for (var parsedModule : parsedModules) {
        checkParsedModule(parsedModule, processedSymbols);
        listener.accept(new CompilationEvent(thisPhase, parsedModule, parsedModule.getSource()));
      }
    }

  }

  private void checkParsedModule(final ParsedModule parsedModule,
                                 final HashMap<String, ISymbol> processedSymbols) {

    final var errorListener = parsedModule.getSource().getErrorListener();
    final var noDuplicatedTraitsOrError = new NoDuplicatedTraitsOrError(errorListener);
    final var scope = parsedModule.getModuleScope();

    for (var symbol : scope.getSymbolsForThisScope()) {
      checkForLoops(errorListener, processedSymbols, symbol);
      checkForDuplicateTraits(noDuplicatedTraitsOrError, symbol);
    }

  }

  private void checkForLoops(final ErrorListener errorListener,
                             final HashMap<String, ISymbol> processedSymbols,
                             final ISymbol symbol) {

    final var classTraitHierarchy = symbol.getGenus().equals(SymbolGenus.CLASS_TRAIT);
    checkForCircularLoops(errorListener, processedSymbols, new HashSet<>(), symbol, classTraitHierarchy);

  }

  private void checkForDuplicateTraits(final NoDuplicatedTraitsOrError checker, final ISymbol symbol) {

    if (symbol instanceof AggregateWithTraitsSymbol aggregate) {
      checker.accept(aggregate);
    }

  }

  private void checkForCircularLoops(final ErrorListener errorListener,
                                     final HashMap<String, ISymbol> processedSymbols,
                                     final HashSet<String> symbolsEncountered,
                                     final ISymbol maybeHasASuper,
                                     final boolean classTraitHierarchy) {

    final var fullyQualifiedName = maybeHasASuper.getFullyQualifiedName();

    if (symbolsEncountered.contains(fullyQualifiedName)) {
      //Well looks like within this hierarchy we've already encountered this symbol.
      final var msg = "with genus '" + maybeHasASuper.getGenus().getDescription() + "' is invalid:";
      errorListener.semanticError(maybeHasASuper.getSourceToken(), msg,
          ErrorListener.SemanticClassification.CIRCULAR_HIERARCHY_DETECTED);
    }

    //Ok, so not encountered within this hierarchy, but has it already been processed?
    //Need to stop the recursion.
    if (processedSymbols.containsKey(fullyQualifiedName)) {
      return; //already checked so stop further recursion now
    }

    //So marked that we've seen this symbol in general terms and
    //Also now we've seen it withing this hierarchy.
    processedSymbols.put(fullyQualifiedName, maybeHasASuper);
    symbolsEncountered.add(fullyQualifiedName);

    if (classTraitHierarchy && maybeHasASuper instanceof AggregateWithTraitsSymbol aggregateWithTraits) {

      aggregateWithTraits.getTraits()
          .forEach(trait -> {
            //Now for each trait we need to copy the symbolsEncountered upto this point as we need to check up 'paths'
            //Rather than the across - this is because that unlike aggregates/functions that can only have one super
            //traits in effect can encounter the same trait. But here we only want to ensure we do not encounter the
            //same trait in a single path from an end trait right back up the path to the base trait, if we do there
            //is a loop.
            var pathSymbolsEncountered = new HashSet<>(symbolsEncountered);
            checkForCircularLoops(errorListener, processedSymbols, pathSymbolsEncountered, trait, true);
          });
      return;
    }

    if (maybeHasASuper instanceof AggregateSymbol aggregate && aggregate.getSuperAggregate().isPresent()) {
      checkForCircularLoops(errorListener, processedSymbols, symbolsEncountered,
          aggregate.getSuperAggregate().get(), classTraitHierarchy);
    } else if (maybeHasASuper instanceof FunctionSymbol function && function.getSuperFunction().isPresent()) {
      checkForCircularLoops(errorListener, processedSymbols, symbolsEncountered,
          function.getSuperFunction().get(), classTraitHierarchy);
    }
    //else it cannot have a super. So that's it.
  }

}
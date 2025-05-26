package org.ek9lang.compiler.phase3;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.DISPATCHERS_ONLY_HAVE_ONE_METHOD_ENTRY_POINT_MARKED;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.DISPATCHER_PRIVATE_IN_SUPER;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.DISPATCHER_PURE_MISMATCH;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.INCOMPATIBLE_PARAMETER_GENUS;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.INVALID_NUMBER_OF_PARAMETERS;

import java.util.List;
import java.util.function.Consumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.support.LocationExtractorFromSymbol;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.symbols.SymbolGenus;

/**
 * Check if any dispatcher methods on the aggregate and ensure that they are valid.
 */
final class ValidDispatcherMethodsOrError extends TypedSymbolAccess implements Consumer<AggregateSymbol> {
  private final LocationExtractorFromSymbol locationExtractorFromSymbol = new LocationExtractorFromSymbol();

  /**
   * Check various aspects of dispatcher methods.
   */
  ValidDispatcherMethodsOrError(final SymbolsAndScopes symbolsAndScopes,
                                final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);

  }

  @Override
  public void accept(final AggregateSymbol aggregateSymbol) {

    if (!aggregateSymbol.isMarkedAsDispatcher()) {
      return; //Nothing to check as it is not marked as a dispatcher
    }

    final var nonAbstractMethodsToCheck = aggregateSymbol.getAllNonAbstractMethods()
        .stream()
        .filter(MethodSymbol::isMarkedAsDispatcher)
        .filter(MethodSymbol::isNotOperator)
        .toList();

    nonAbstractMethodsToCheck.forEach(methodSymbol -> validateDispatcherMethod(aggregateSymbol, methodSymbol));

  }


  private void validateDispatcherMethod(final AggregateSymbol aggregateSymbol, final MethodSymbol methodSymbol) {

    //Just an initial basic check first. Is it even a valid dispatcher.
    var numMethodParameters = methodSymbol.getCallParameters().size();
    if (numMethodParameters < 1 || numMethodParameters > 2) {
      var msg = String.format("number of parameters %d, expect one or two for dispatcher method:", numMethodParameters);
      errorListener.semanticError(methodSymbol.getSourceToken(), msg, INVALID_NUMBER_OF_PARAMETERS);
      return;
    }

    validMethodOrError(aggregateSymbol, methodSymbol, numMethodParameters);

  }

  private void validMethodOrError(final AggregateSymbol aggregateSymbol,
                                  final MethodSymbol methodSymbol,
                                  final int numMethodParameters) {

    var methodsOfTheSameName = getAllOtherMethodsWithSameName(aggregateSymbol, methodSymbol);

    //Don't test method against itself.
    for (var matchedMethodSymbol : methodsOfTheSameName) {

      var numMethodParametersOnMatchedMethod = matchedMethodSymbol.getCallParameters().size();
      var msg = getErrorMessageFor(matchedMethodSymbol, methodSymbol);

      //Number of arguments must match up
      if (numMethodParameters != numMethodParametersOnMatchedMethod) {
        errorListener.semanticError(matchedMethodSymbol.getSourceToken(), msg, INVALID_NUMBER_OF_PARAMETERS);
      }

      //Purity markers must match up
      if (methodSymbol.isMarkedPure() && matchedMethodSymbol.isNotMarkedPure()) {
        errorListener.semanticError(matchedMethodSymbol.getSourceToken(), msg, DISPATCHER_PURE_MISMATCH);
      }

      //If matched method is private and in a super - then it can never be called - because it is private!
      //But this is very likely to be misleading for the developer - think that it will get called.
      if (matchedMethodSymbol.isPrivate()
          && matchedMethodSymbol.getParentScope() instanceof IAggregateSymbol matchedParentAggregate
          && aggregateSymbol != matchedParentAggregate) {
        errorListener.semanticError(matchedMethodSymbol.getSourceToken(), msg, DISPATCHER_PRIVATE_IN_SUPER);
      }

      if (matchedMethodSymbol.isMarkedAsDispatcher()) {
        //Only one method at the entry should be marked as a dispatcher
        errorListener.semanticError(matchedMethodSymbol.getSourceToken(), msg,
            DISPATCHERS_ONLY_HAVE_ONE_METHOD_ENTRY_POINT_MARKED);
      }

      validateParameterGenus(methodSymbol, matchedMethodSymbol);
    }
  }

  private void validateParameterGenus(final MethodSymbol methodSymbol, final MethodSymbol matchedMethodSymbol) {

    var numParametersDispatcherMethod = methodSymbol.getCallParameters().size();
    var numParametersMatchedMethod = matchedMethodSymbol.getCallParameters().size();

    if (numParametersMatchedMethod == numParametersDispatcherMethod) {
      var msg = getErrorMessageFor(matchedMethodSymbol, methodSymbol);
      for (int i = 0; i < numParametersDispatcherMethod; i++) {
        final var dispatcherArg = methodSymbol.getCallParameters().get(i);
        final var matchedArg = matchedMethodSymbol.getCallParameters().get(i);
        dispatcherArg.getType().ifPresent(dispatcherArgType -> matchedArg.getType().ifPresent(matchedArgType -> {
          if (!genusCompatible(dispatcherArgType, matchedArgType)) {
            var fullMsg = dispatcherArgType.getGenus() + " vs " + matchedArgType.getGenus() + ": " + msg;
            errorListener.semanticError(matchedArg.getSourceToken(), fullMsg, INCOMPATIBLE_PARAMETER_GENUS);
          }
        }));
      }
    }

  }

  private boolean genusCompatible(final ISymbol dispatcherArgType, final ISymbol matchedArgType) {
    final var dispatcherArgTypeGenus = dispatcherArgType.getGenus();
    final var matchedArgTypeGenus = matchedArgType.getGenus();
    if (dispatcherArgTypeGenus.equals(matchedArgTypeGenus)) {
      return true;
    }

    if (dispatcherArgTypeGenus.equals(SymbolGenus.CLASS) && matchedArgTypeGenus.equals(SymbolGenus.CLASS_TRAIT)
        || dispatcherArgTypeGenus.equals(SymbolGenus.CLASS_TRAIT) && matchedArgTypeGenus.equals(SymbolGenus.CLASS)) {
      return true;
    }
    return dispatcherArgTypeGenus.equals(SymbolGenus.ANY);
  }

  private List<MethodSymbol> getAllOtherMethodsWithSameName(final AggregateSymbol aggregateSymbol,
                                                            final MethodSymbol methodSymbol) {
    return aggregateSymbol.getAllMethods()
        .stream()
        .filter(MethodSymbol::isNotConstructor)
        .filter(MethodSymbol::isNotMarkedAbstract)
        .filter(MethodSymbol::isNotOperator)
        .filter(method -> method != methodSymbol)
        .filter(method -> method.getName().equals(methodSymbol.getName())).toList();
  }

  private String getErrorMessageFor(final MethodSymbol methodSymbol1,
                                    final MethodSymbol methodSymbol2) {

    final var message = String.format("'%s' %s:",
        methodSymbol2.getFriendlyName(), locationExtractorFromSymbol.apply(methodSymbol2));

    return "'" + methodSymbol1.getFriendlyName() + "' and " + message;
  }

}

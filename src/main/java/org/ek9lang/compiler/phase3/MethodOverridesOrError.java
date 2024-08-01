package org.ek9lang.compiler.phase3;

import java.util.List;
import java.util.function.Consumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.search.MethodSymbolSearchResult;
import org.ek9lang.compiler.support.LocationExtractorFromSymbol;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;

/**
 * Check overrides on methods.
 */
final class MethodOverridesOrError extends TypedSymbolAccess implements Consumer<AggregateSymbol> {
  private final TypeCovarianceOrError typeCovarianceOrError;
  private final ErrorListener.SemanticClassification errorWhenShouldBeMarkedAbstract;
  private final PureModifierOrError pureModifierOrError;
  private final LocationExtractorFromSymbol locationExtractorFromSymbol = new LocationExtractorFromSymbol();
  private final TraverseAbstractMethods traverseAbstractMethods
      = new TraverseAbstractMethods();

  /**
   * Check various aspects of overriding methods.
   */
  MethodOverridesOrError(final SymbolsAndScopes symbolsAndScopes,
                         final ErrorListener errorListener,
                         final ErrorListener.SemanticClassification errorWhenShouldBeMarkedAbstract) {

    super(symbolsAndScopes, errorListener);
    this.typeCovarianceOrError = new TypeCovarianceOrError(symbolsAndScopes, errorListener);
    this.errorWhenShouldBeMarkedAbstract = errorWhenShouldBeMarkedAbstract;
    this.pureModifierOrError = new PureModifierOrError(symbolsAndScopes, errorListener);

  }

  @Override
  public void accept(final AggregateSymbol aggregateSymbol) {

    final var nonAbstractMethodsToCheck = aggregateSymbol.getAllNonAbstractMethodsInThisScopeOnly();
    final var abstractMethodsToCheck = aggregateSymbol.getAllAbstractMethodsInThisScopeOnly();

    nonAbstractMethodsToCheck.forEach(methodSymbol -> validMethodInSuperAndTraitsOrError(methodSymbol,
        aggregateSymbol));

    pureAndConstructorsConsistentOrError(nonAbstractMethodsToCheck);

    abstractMethodsToCheck.forEach(methodSymbol -> validMethodInSuperAndTraitsOrError(methodSymbol, aggregateSymbol));

    if (!aggregateSymbol.isMarkedAbstract()) {
      validAbstractnessOrError(aggregateSymbol);
    }

  }

  /**
   * If there is one constructor marked as pure then all must be pared as pure.
   */
  private void pureAndConstructorsConsistentOrError(final List<MethodSymbol> methodsToCheck) {

    final var numberOfConstructors = methodsToCheck
        .stream()
        .filter(MethodSymbol::isConstructor)
        .count();

    final var numberNotMarkedPure = methodsToCheck
        .stream()
        .filter(MethodSymbol::isConstructor)
        .filter(MethodSymbol::isNotMarkedPure)
        .count();

    //This means that one or more must be marked pure.
    if (numberOfConstructors != numberNotMarkedPure && numberNotMarkedPure != 0) {
      methodsToCheck
          .stream()
          .filter(MethodSymbol::isConstructor)
          .filter(MethodSymbol::isNotMarkedPure)
          .forEach(
              nonPureConstructor -> errorListener.semanticError(nonPureConstructor.getSourceToken(), "",
                  ErrorListener.SemanticClassification.MIX_OF_PURE_AND_NOT_PURE_CONSTRUCTORS)
          );
    }
  }

  /**
   * Workout if there are abstract methods that have not been implemented.
   * For a non-abstract type that is an error.
   */
  private void validAbstractnessOrError(final AggregateSymbol aggregateSymbol) {

    Consumer<MethodSymbol> actionToTake = match -> {
      if (match.isMarkedAbstract()) {
        final var location = locationExtractorFromSymbol.apply(match);
        final var errorMessage = "'" + match.getFriendlyName() + "' " + location + " not overridden:";
        errorListener.semanticError(aggregateSymbol.getSourceToken(), errorMessage, errorWhenShouldBeMarkedAbstract);
      }
    };

    traverseAbstractMethods.accept(aggregateSymbol, actionToTake);

  }

  private void validMethodInSuperAndTraitsOrError(final MethodSymbol methodSymbol,
                                                  final AggregateSymbol aggregateSymbol) {

    boolean methodFound = aggregateSymbol
        .getSuperAggregate()
        .filter(theSuper -> validMethodOrError(methodSymbol, theSuper))
        .isPresent();

    if (!methodFound) {
      methodFound = aggregateSymbol
          .getTraits()
          .stream()
          .anyMatch(trait -> validMethodOrError(methodSymbol, trait));
    }

    if (!methodFound && methodSymbol.isOverride()) {
      errorListener.semanticError(methodSymbol.getSourceToken(), "",
          ErrorListener.SemanticClassification.DOES_NOT_OVERRIDE);
    }

  }

  /**
   * Checks is the aggregate has the method, if it does then check override is specified.
   * But if not found in the aggregate no error is issued.
   * This is left to the calling method, because it is important to check supers and aggregates
   * and only of nothing found maybe issue error.
   */
  private boolean validMethodOrError(final MethodSymbol methodSymbol,
                                     final IAggregateSymbol superAggregateSymbol) {

    final var search = new MethodSymbolSearch(methodSymbol);
    final var result = superAggregateSymbol.resolveMatchingMethods(search, new MethodSymbolSearchResult());

    result.getSingleBestMatchSymbol().ifPresent(match -> {

      final var errorMessage = getErrorMessageFor(methodSymbol, match);
      validMethodAccessModifierCompatibilityOrError(methodSymbol, match);
      //If the super is private then it is not being overridden and so covariance and override does not need checking.
      if (!match.isPrivate()) {
        validCovarianceOnMethodReturnTypesOrError(methodSymbol, match);
        validOverrideRequiredOrError(methodSymbol);
        pureModifierOrError.accept(new PureCheckData(errorMessage, match, methodSymbol));
      } else if (methodSymbol.isOverride()) {
        errorListener.semanticError(methodSymbol.getSourceToken(), errorMessage,
            ErrorListener.SemanticClassification.DOES_NOT_OVERRIDE);
      }
    });

    return result.getSingleBestMatchSymbol().isPresent();
  }

  private void validMethodAccessModifierCompatibilityOrError(final MethodSymbol methodSymbol,
                                                             final MethodSymbol matchedMethodSymbol) {
    if (!matchedMethodSymbol.isPrivate()
        && !methodSymbol.getAccessModifier().equals(matchedMethodSymbol.getAccessModifier())) {

      final var msg = getErrorMessageFor(methodSymbol, matchedMethodSymbol);
      errorListener.semanticError(methodSymbol.getSourceToken(), msg,
          ErrorListener.SemanticClassification.METHOD_ACCESS_MODIFIERS_DIFFER);

    }
  }

  private void validOverrideRequiredOrError(final MethodSymbol methodSymbol) {

    if (!methodSymbol.isSynthetic() && !methodSymbol.isOverride()) {
      errorListener.semanticError(methodSymbol.getSourceToken(), "'override' required on '" + methodSymbol + "'",
          ErrorListener.SemanticClassification.METHOD_OVERRIDES);
    } else {
      //So if the ek9 compiler synthetically generated the method it too must mark it with override.
      methodSymbol.setOverride(true);
    }

  }

  private void validCovarianceOnMethodReturnTypesOrError(final MethodSymbol methodSymbol,
                                                         final MethodSymbol matchedMethodSymbol) {

    final var errorMessage = getErrorMessageFor(methodSymbol, matchedMethodSymbol);
    final var data = new CovarianceData(methodSymbol.getSourceToken(), errorMessage,
        methodSymbol.getReturningSymbol(), matchedMethodSymbol.getReturningSymbol());

    typeCovarianceOrError.accept(data);

  }

  private String getErrorMessageFor(final MethodSymbol methodSymbol,
                                    final MethodSymbol matchedMethodSymbol) {

    final var message = String.format("'%s' %s:",
        matchedMethodSymbol.getFriendlyName(), locationExtractorFromSymbol.apply(matchedMethodSymbol));

    return "'" + methodSymbol.getFriendlyName() + "' and " + message;
  }
}

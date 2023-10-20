package org.ek9lang.compiler.phase3;

import java.util.List;
import java.util.function.Consumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.search.MethodSymbolSearchResult;
import org.ek9lang.compiler.support.LocationExtractor;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;

/**
 * Check overrides on methods.
 */
final class CheckMethodOverrides extends TypedSymbolAccess implements Consumer<AggregateSymbol> {
  private final CheckTypeCovariance checkTypeCovariance;
  private final ErrorListener.SemanticClassification errorWhenShouldBeMarkedAbstract;
  private final CheckPureModifier checkPureModifier;
  private final LocationExtractor locationExtractor = new LocationExtractor();

  /**
   * Check various aspects of overriding methods.
   */
  CheckMethodOverrides(final SymbolAndScopeManagement symbolAndScopeManagement,
                       final ErrorListener errorListener,
                       final ErrorListener.SemanticClassification errorWhenShouldBeMarkedAbstract) {
    super(symbolAndScopeManagement, errorListener);
    this.checkTypeCovariance = new CheckTypeCovariance(symbolAndScopeManagement, errorListener);
    this.errorWhenShouldBeMarkedAbstract = errorWhenShouldBeMarkedAbstract;
    this.checkPureModifier = new CheckPureModifier(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(final AggregateSymbol aggregateSymbol) {

    List<MethodSymbol> nonAbstractMethodsToCheck = aggregateSymbol.getAllNonAbstractMethodsInThisScopeOnly();
    nonAbstractMethodsToCheck.forEach(methodSymbol -> checkMethodInSuperAndTraits(methodSymbol,
        aggregateSymbol));

    checkPureAndConstructorsIsConsistent(nonAbstractMethodsToCheck);

    List<MethodSymbol> abstractMethodsToCheck = aggregateSymbol.getAllAbstractMethodsInThisScopeOnly();
    abstractMethodsToCheck.forEach(methodSymbol -> checkMethodInSuperAndTraits(methodSymbol, aggregateSymbol));

    if (!aggregateSymbol.isMarkedAbstract()) {
      checkAbstractness(aggregateSymbol);
    }
  }

  /**
   * If there is one constructor marked as pure then all must be pared as pure.
   */
  private void checkPureAndConstructorsIsConsistent(List<MethodSymbol> methodsToCheck) {
    var numberOfConstructors = methodsToCheck
        .stream()
        .filter(MethodSymbol::isConstructor)
        .count();
    var numberNotMarkedPure = methodsToCheck
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
  private void checkAbstractness(final AggregateSymbol aggregateSymbol) {
    //Here we need all the abstract methods to check that they have been override by something non-abstract.
    final List<MethodSymbol> abstractMethodsToCheck = aggregateSymbol.getAllAbstractMethods();

    abstractMethodsToCheck.forEach(methodSymbol -> {
      MethodSymbolSearch search = new MethodSymbolSearch(methodSymbol);
      var result = aggregateSymbol.resolveMatchingMethods(search, new MethodSymbolSearchResult());
      result.getSingleBestMatchSymbol().ifPresent(match -> {
        if (match.isMarkedAbstract()) {
          var location = locationExtractor.apply(match);
          var errorMessage = "'" + match.getFriendlyName() + "' " + location + " not overridden:";
          errorListener.semanticError(aggregateSymbol.getSourceToken(), errorMessage, errorWhenShouldBeMarkedAbstract);
        }
      });
    });
  }

  private void checkMethodInSuperAndTraits(final MethodSymbol methodSymbol,
                                           final AggregateSymbol aggregateSymbol) {
    aggregateSymbol.getSuperAggregateSymbol().ifPresent(theSuper -> checkMethod(methodSymbol, theSuper));

    for (IAggregateSymbol trait : aggregateSymbol.getTraits()) {
      checkMethod(methodSymbol, trait);
    }
  }

  private void checkMethod(final MethodSymbol methodSymbol,
                           final IAggregateSymbol superAggregateSymbol) {
    MethodSymbolSearch search = new MethodSymbolSearch(methodSymbol);

    var result = superAggregateSymbol.resolveMatchingMethods(search, new MethodSymbolSearchResult());
    result.getSingleBestMatchSymbol().ifPresentOrElse(match -> {
      var errorMessage = getErrorMessageFor(methodSymbol, match);
      checkMethodAccessModifierCompatibility(methodSymbol, match);
      //If the super is private then it is not being overridden and so covariance and override does not need checking.
      if (!match.isPrivate()) {
        checkCovarianceOnMethodReturnTypes(methodSymbol, match);
        checkIfOverrideRequired(methodSymbol);
        checkPureModifier.accept(new PureCheckData(errorMessage, match, methodSymbol));
      } else if (methodSymbol.isOverride()) {
        errorListener.semanticError(methodSymbol.getSourceToken(), errorMessage,
            ErrorListener.SemanticClassification.DOES_NOT_OVERRIDE);
      }
    }, () -> {
      //So there are no methods in the super that match this method, better check it's not been marked as overriding
      if (methodSymbol.isOverride()) {
        errorListener.semanticError(methodSymbol.getSourceToken(), "",
            ErrorListener.SemanticClassification.DOES_NOT_OVERRIDE);
      }
    });
  }

  private void checkMethodAccessModifierCompatibility(final MethodSymbol methodSymbol,
                                                      final MethodSymbol matchedMethodSymbol) {
    if (!matchedMethodSymbol.isPrivate()
        && !methodSymbol.getAccessModifier().equals(matchedMethodSymbol.getAccessModifier())) {
      var msg = getErrorMessageFor(methodSymbol, matchedMethodSymbol);
      errorListener.semanticError(methodSymbol.getSourceToken(), msg,
          ErrorListener.SemanticClassification.METHOD_ACCESS_MODIFIERS_DIFFER);
    }
  }

  private void checkIfOverrideRequired(final MethodSymbol methodSymbol) {
    if (!methodSymbol.isSynthetic() && !methodSymbol.isOverride()) {
      errorListener.semanticError(methodSymbol.getSourceToken(), "'override' required on '" + methodSymbol + "'",
          ErrorListener.SemanticClassification.METHOD_OVERRIDES);
    } else {
      //So if the ek9 compiler synthetically generated the method it too must mark it with override.
      methodSymbol.setOverride(true);
    }
  }

  private void checkCovarianceOnMethodReturnTypes(final MethodSymbol methodSymbol,
                                                  final MethodSymbol matchedMethodSymbol) {
    var errorMessage = getErrorMessageFor(methodSymbol, matchedMethodSymbol);

    CovarianceCheckData data = new CovarianceCheckData(methodSymbol.getSourceToken(), errorMessage,
        methodSymbol.getReturningSymbol(), matchedMethodSymbol.getReturningSymbol());
    checkTypeCovariance.accept(data);
  }

  private String getErrorMessageFor(final MethodSymbol methodSymbol,
                                    final MethodSymbol matchedMethodSymbol) {
    String message = String.format("'%s' %s:",
        matchedMethodSymbol.getFriendlyName(), locationExtractor.apply(matchedMethodSymbol));

    return "'" + methodSymbol.getFriendlyName() + "' and " + message;
  }
}

package org.ek9lang.compiler.main.resolvedefine;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.main.phases.definition.SymbolAndScopeManagement;
import org.ek9lang.compiler.main.rules.CheckTypeCovariance;
import org.ek9lang.compiler.main.rules.CovarianceCheckData;
import org.ek9lang.compiler.support.RuleSupport;
import org.ek9lang.compiler.symbol.AggregateSymbol;
import org.ek9lang.compiler.symbol.IAggregateSymbol;
import org.ek9lang.compiler.symbol.MethodSymbol;
import org.ek9lang.compiler.symbol.support.search.MethodSymbolSearch;
import org.ek9lang.compiler.symbol.support.search.MethodSymbolSearchResult;

/**
 * Check overrides on methods.
 * TODO Check the number and compatibility of arguments on methods that are overridden.
 */
public class CheckMethodOverrides extends RuleSupport implements Consumer<AggregateSymbol> {

  private final CheckTypeCovariance checkTypeCovariance;

  /**
   * Check various aspects of overriding methods.
   */
  public CheckMethodOverrides(final SymbolAndScopeManagement symbolAndScopeManagement,
                              final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
    this.checkTypeCovariance = new CheckTypeCovariance(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(final AggregateSymbol aggregateSymbol) {
    List<MethodSymbol> symbolsToCheck = aggregateSymbol.getAllNonAbstractMethodsInThisScopeOnly();
    symbolsToCheck.forEach(methodSymbol -> checkMethodInSuperAndTraits(methodSymbol, aggregateSymbol));
  }

  private void checkMethodInSuperAndTraits(final MethodSymbol methodSymbol,
                                           final AggregateSymbol aggregateSymbol) {
    aggregateSymbol.getSuperAggregateSymbol().ifPresent(theSuper -> checkMethods(methodSymbol, theSuper));

    for (IAggregateSymbol trait : aggregateSymbol.getTraits()) {
      checkMethods(methodSymbol, trait);
    }
  }

  private void checkMethods(final MethodSymbol methodSymbol,
                            final IAggregateSymbol aggregateSymbol) {
    MethodSymbolSearch search = new MethodSymbolSearch(methodSymbol);

    var result = aggregateSymbol.resolveMatchingMethods(search, new MethodSymbolSearchResult());
    result.getSingleBestMatchSymbol().ifPresent(match -> {
      checkMethodAccessModifierCompatibility(methodSymbol, match);
      //If the super is private then it is not being overridden and so covariance and override does not need checking.
      if (!match.isPrivate()) {
        checkCovarianceOnMethodReturnTypes(methodSymbol, match);
        checkIfOverrideRequired(methodSymbol);
      } else if (methodSymbol.isOverride()) {
        //It is private as so this method must not use override key word else that too is an error
        var msg = getErrorMessageFor(methodSymbol, match);
        errorListener.semanticError(methodSymbol.getSourceToken(), msg,
            ErrorListener.SemanticClassification.METHOD_DOES_NOT_OVERRIDE);
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
    var msg = getErrorMessageFor(methodSymbol, matchedMethodSymbol);

    CovarianceCheckData data = new CovarianceCheckData(methodSymbol.getSourceToken(), msg,
        methodSymbol.getReturningSymbol(), matchedMethodSymbol.getReturningSymbol());
    checkTypeCovariance.accept(data);

  }

  private String getErrorMessageFor(final MethodSymbol methodSymbol,
                                    final MethodSymbol matchedMethodSymbol) {
    String message = String.format("'%s' on line %d in %s:",
        matchedMethodSymbol.getFriendlyName(), matchedMethodSymbol.getSourceToken().getLine(),
        new File(matchedMethodSymbol.getSourceFileLocation()).getName());

    return "'" + methodSymbol.getFriendlyName() + "' and " + message;
  }
}

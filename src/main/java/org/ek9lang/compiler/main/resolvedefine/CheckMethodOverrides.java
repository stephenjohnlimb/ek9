package org.ek9lang.compiler.main.resolvedefine;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.main.phases.definition.SymbolAndScopeManagement;
import org.ek9lang.compiler.support.RuleSupport;
import org.ek9lang.compiler.symbol.AggregateSymbol;
import org.ek9lang.compiler.symbol.IAggregateSymbol;
import org.ek9lang.compiler.symbol.MethodSymbol;
import org.ek9lang.compiler.symbol.support.search.MethodSymbolSearch;
import org.ek9lang.compiler.symbol.support.search.MethodSymbolSearchResult;

/**
 * Check overrides on methods.
 */
public class CheckMethodOverrides extends RuleSupport implements Consumer<AggregateSymbol> {
  public CheckMethodOverrides(final SymbolAndScopeManagement symbolAndScopeManagement,
                              final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
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
      }
    });
  }

  private void checkMethodAccessModifierCompatibility(final MethodSymbol methodSymbol,
                                                      final MethodSymbol matchedMethodSymbol) {

    if (!matchedMethodSymbol.isPrivate()
        && !methodSymbol.getAccessModifier().equals(matchedMethodSymbol.getAccessModifier())) {
      String message = String.format("'%s' on line %d in %s:",
          matchedMethodSymbol.getFriendlyName(), matchedMethodSymbol.getSourceToken().getLine(),
          new File(matchedMethodSymbol.getSourceFileLocation()).getName());

      var msg = "'" + methodSymbol.getFriendlyName() + "' and " + message;
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

  private void checkCovarianceOnMethodReturnTypes(final MethodSymbol method, final MethodSymbol matchedMethod) {
    //TODO check that matchedMethod return type and method return type for compatibility.
  }
}

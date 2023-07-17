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
import org.ek9lang.core.exception.CompilerException;

/**
 * Check overrides on methods.
 */
public class CheckMethodOverrides extends RuleSupport implements Consumer<AggregateSymbol> {

  private final CheckTypeCovariance checkTypeCovariance;

  private final ErrorListener.SemanticClassification errorWhenShouldBeMarkedAbstract;

  /**
   * Check various aspects of overriding methods.
   */
  public CheckMethodOverrides(final SymbolAndScopeManagement symbolAndScopeManagement,
                              final ErrorListener errorListener,
                              final ErrorListener.SemanticClassification errorWhenShouldBeMarkedAbstract) {
    super(symbolAndScopeManagement, errorListener);
    this.checkTypeCovariance = new CheckTypeCovariance(symbolAndScopeManagement, errorListener);
    this.errorWhenShouldBeMarkedAbstract = errorWhenShouldBeMarkedAbstract;
  }

  @Override
  public void accept(final AggregateSymbol aggregateSymbol) {

    List<MethodSymbol> nonAbstractMethodsToCheck = aggregateSymbol.getAllNonAbstractMethodsInThisScopeOnly();
    nonAbstractMethodsToCheck.forEach(methodSymbol -> checkMethodInSuperAndTraits(methodSymbol,
        aggregateSymbol));

    List<MethodSymbol> abstractMethodsToCheck = aggregateSymbol.getAllAbstractMethodsInThisScopeOnly();
    abstractMethodsToCheck.forEach(methodSymbol -> checkMethodInSuperAndTraits(methodSymbol, aggregateSymbol));

    if (!aggregateSymbol.isMarkedAbstract()) {
      checkAbstractness(aggregateSymbol);
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
      result.getSingleBestMatchSymbol().ifPresentOrElse(match -> {
        if (match.isMarkedAbstract()) {
          var errorMessage = "'" + match.getFriendlyName() + "' not overridden:";
          errorListener.semanticError(aggregateSymbol.getSourceToken(), errorMessage, errorWhenShouldBeMarkedAbstract);
        }
      }, () -> {
        //So how is this possible? Compiler error!
        throw new CompilerException("Some how unable to resolve method for [" + methodSymbol.getFriendlyName() + "]");
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
        checkIfPureIsRequired(errorMessage, methodSymbol, match);
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

  private void checkIfPureIsRequired(final String errorMessage,
                                     final MethodSymbol methodSymbol,
                                     final MethodSymbol match) {
    if (methodSymbol.isMarkedPure() && !match.isMarkedPure()) {
      errorListener.semanticError(methodSymbol.getSourceToken(), errorMessage,
          ErrorListener.SemanticClassification.SUPER_IS_NOT_PURE);
    } else if (!methodSymbol.isMarkedPure() && match.isMarkedPure()) {
      errorListener.semanticError(methodSymbol.getSourceToken(), errorMessage,
          ErrorListener.SemanticClassification.SUPER_IS_PURE);
    }
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
    String message = String.format("'%s' on line %d in %s:",
        matchedMethodSymbol.getFriendlyName(), matchedMethodSymbol.getSourceToken().getLine(),
        new File(matchedMethodSymbol.getSourceFileLocation()).getName());

    return "'" + methodSymbol.getFriendlyName() + "' and " + message;
  }
}

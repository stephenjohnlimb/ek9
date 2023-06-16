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
import org.ek9lang.core.exception.AssertValue;

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
    //So first check they both return 'something' if both nothing - then no covariance to consider.
    //If one does and one does not - well that's not going to work in terms of covariance.
    var msg = getErrorMessageFor(methodSymbol, matchedMethodSymbol);

    if (methodSymbol.isReturningSymbolPresent() && matchedMethodSymbol.isReturningSymbolPresent()) {
      //But if they both return something - then lets check the types and see if they are compatible (without coercion).
      var methodReturnType = methodSymbol.getReturningSymbol().getType();
      var matchedMethodReturnType = matchedMethodSymbol.getReturningSymbol().getType();
      //A couple of paranoid checks - while we are still developing.
      AssertValue.checkTrue("Compiler Error", methodReturnType.isPresent());
      AssertValue.checkTrue("Compiler Error", matchedMethodReturnType.isPresent());

      //Check using no coercion - for compatibility.
      var assignableWeight = methodReturnType.get().getUnCoercedAssignableWeightTo(matchedMethodReturnType.get());
      if (assignableWeight < 0.0) {
        errorListener.semanticError(methodSymbol.getSourceToken(), "incompatible return types; " + msg,
            ErrorListener.SemanticClassification.COVARIANCE_MISMATCH);
      }

    } else if (!methodSymbol.isReturningSymbolPresent() && matchedMethodSymbol.isReturningSymbolPresent()) {
      //Cannot alter return to be Void (nothing)
      errorListener.semanticError(methodSymbol.getSourceToken(), "missing return type/value; " + msg,
          ErrorListener.SemanticClassification.COVARIANCE_MISMATCH);
    } else if (methodSymbol.isReturningSymbolPresent() && !matchedMethodSymbol.isReturningSymbolPresent()) {
      //Cannot do reverse either if base was Void cannot no add a return type
      errorListener.semanticError(methodSymbol.getSourceToken(), "unexpected return type/value; " + msg,
          ErrorListener.SemanticClassification.COVARIANCE_MISMATCH);
    }
    //else both not returning a value so nothing to check
  }

  private String getErrorMessageFor(final MethodSymbol methodSymbol,
                                    final MethodSymbol matchedMethodSymbol) {
    String message = String.format("'%s' on line %d in %s:",
        matchedMethodSymbol.getFriendlyName(), matchedMethodSymbol.getSourceToken().getLine(),
        new File(matchedMethodSymbol.getSourceFileLocation()).getName());

    return "'" + methodSymbol.getFriendlyName() + "' and " + message;
  }
}

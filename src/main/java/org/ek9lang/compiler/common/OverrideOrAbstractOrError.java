package org.ek9lang.compiler.common;

import java.util.function.Consumer;
import org.ek9lang.compiler.symbols.MethodSymbol;

/**
 * Checks for both abstract and override. As this makes no sense.
 */
public class OverrideOrAbstractOrError extends RuleSupport implements Consumer<MethodSymbol> {

  /**
   * Create new checker.
   */
  public OverrideOrAbstractOrError(final SymbolAndScopeManagement symbolAndScopeManagement,
                                   final ErrorListener errorListener) {

    super(symbolAndScopeManagement, errorListener);

  }

  @Override
  public void accept(final MethodSymbol methodSymbol) {

    if (methodSymbol.isMarkedAbstract() && methodSymbol.isOverride()) {
      //This makes no sense to define a method abstract and also say it overrides something.
      errorListener.semanticError(methodSymbol.getSourceToken(), "",
          ErrorListener.SemanticClassification.OVERRIDE_AND_ABSTRACT);
    }

  }
}

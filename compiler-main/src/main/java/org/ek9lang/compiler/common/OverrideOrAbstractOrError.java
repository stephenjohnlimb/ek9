package org.ek9lang.compiler.common;

import java.util.function.Consumer;
import org.ek9lang.compiler.symbols.MethodSymbol;

/**
 * Checks for both abstract and override. As both in combination makes no sense.
 */
public class OverrideOrAbstractOrError extends RuleSupport implements Consumer<MethodSymbol> {

  private final ExternallyImplemented externallyImplemented = new ExternallyImplemented();

  /**
   * Create new checker.
   */
  public OverrideOrAbstractOrError(final SymbolsAndScopes symbolsAndScopes,
                                   final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);

  }

  @Override
  public void accept(final MethodSymbol methodSymbol) {

    //If this method is externally defined package/code then no need to check this.
    if (externallyImplemented.test(methodSymbol)) {
      return;
    }

    if (methodSymbol.isMarkedAbstract() && methodSymbol.isOverride()) {
      //This makes no sense to define a method abstract and also say it overrides something.

      errorListener.semanticError(methodSymbol.getSourceToken(), "",
          ErrorListener.SemanticClassification.OVERRIDE_AND_ABSTRACT);
    }

  }
}

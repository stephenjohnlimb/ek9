package org.ek9lang.compiler.main.rules;

import java.util.function.Consumer;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.main.phases.definition.SymbolAndScopeManagement;
import org.ek9lang.compiler.support.RuleSupport;
import org.ek9lang.compiler.symbol.MethodSymbol;

/**
 * Checks for both abstract and override. As this makes no sense.
 */
public class CheckOverrideAndAbstract extends RuleSupport implements Consumer<MethodSymbol> {
  protected CheckOverrideAndAbstract(
      SymbolAndScopeManagement symbolAndScopeManagement,
      ErrorListener errorListener) {
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

package org.ek9lang.compiler.support;

import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.core.AssertValue;

/**
 * Designed to be extended by rules and checkers.
 */
public abstract class RuleSupport {
  protected final SymbolAndScopeManagement symbolAndScopeManagement;

  protected final ErrorListener errorListener;

  protected RuleSupport(final SymbolAndScopeManagement symbolAndScopeManagement,
                        final ErrorListener errorListener) {

    AssertValue.checkNotNull("symbolAndScopeManagement cannot be null", symbolAndScopeManagement);
    AssertValue.checkNotNull("errorListener cannot be null", errorListener);

    this.symbolAndScopeManagement = symbolAndScopeManagement;
    this.errorListener = errorListener;
  }
}

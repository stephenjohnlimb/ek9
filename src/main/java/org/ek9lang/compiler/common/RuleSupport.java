package org.ek9lang.compiler.common;

import org.ek9lang.core.AssertValue;

/**
 * Designed to be extended by rules and checkers.
 */
public abstract class RuleSupport {
  protected final SymbolsAndScopes symbolsAndScopes;
  protected final ErrorListener errorListener;

  protected RuleSupport(final SymbolsAndScopes symbolsAndScopes,
                        final ErrorListener errorListener) {

    AssertValue.checkNotNull("symbolsAndScopes cannot be null", symbolsAndScopes);
    AssertValue.checkNotNull("errorListener cannot be null", errorListener);

    this.symbolsAndScopes = symbolsAndScopes;
    this.errorListener = errorListener;
  }
}

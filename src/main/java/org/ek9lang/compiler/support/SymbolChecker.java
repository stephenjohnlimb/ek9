package org.ek9lang.compiler.support;

import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.search.SymbolSearch;
import org.ek9lang.compiler.symbols.IScope;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.core.AssertValue;

/**
 * General utility class that searches in a scope for a symbol to check for duplicates.
 * Note that is a bit ore than a boolean check. It will formulate error messages and add
 * then to the error listener it has been provided with.
 */
public class SymbolChecker {

  private final NoNameCollisionOrError noNameCollisionOrError;

  public SymbolChecker(final ErrorListener errorListener) {

    this.noNameCollisionOrError = new NoNameCollisionOrError(errorListener, true);

  }

  /**
   * Check for exising symbol in the scope.
   * if returns true then errors will have been added to the error listener.
   */
  public boolean errorsIfSymbolAlreadyDefined(final IScope inScope,
                                              final ISymbol symbol,
                                              final boolean limitVarSearchToBlockScope) {

    AssertValue.checkNotNull("Scope cannot be null", inScope);
    AssertValue.checkNotNull("Symbol cannot be null", symbol);

    if (!noNameCollisionOrError.test(inScope, symbol)) {
      return noNameCollisionOrError.errorsIfResolved(inScope, symbol,
          new SymbolSearch(symbol.getName()).setLimitToBlocks(limitVarSearchToBlockScope),
          ErrorListener.SemanticClassification.DUPLICATE_VARIABLE);
    }

    return true;
  }
}

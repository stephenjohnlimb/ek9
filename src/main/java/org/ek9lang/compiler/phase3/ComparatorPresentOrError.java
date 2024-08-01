package org.ek9lang.compiler.phase3;

import java.util.function.BiPredicate;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * Given a symbol, this check that the type that the symbol has can support the '&lt;=&gt;' operator.
 */
class ComparatorPresentOrError extends OperatorTest implements BiPredicate<IToken, ISymbol> {

  ComparatorPresentOrError(final SymbolsAndScopes symbolsAndScopes,
                           final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);

  }

  @Override
  protected MethodSymbolSearch getMethodSymbolSearch(final ISymbol symbolType) {

    return new MethodSymbolSearch("<=>")
        .addTypeParameter(symbolType)
        .setOfTypeOrReturn(symbolsAndScopes.getEk9Types().ek9Integer());
  }
}

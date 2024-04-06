package org.ek9lang.compiler.phase3;

import java.util.function.BiPredicate;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * Given a symbol, this check that the type that the symbol has can support the '&lt;=&gt;' operation.
 */
class CheckForComparator extends OperatorCheck implements BiPredicate<IToken, ISymbol> {

  CheckForComparator(final SymbolAndScopeManagement symbolAndScopeManagement,
                     final ErrorListener errorListener) {

    super(symbolAndScopeManagement, errorListener);

  }

  @Override
  protected MethodSymbolSearch getMethodSymbolSearch(final ISymbol symbolType) {

    return new MethodSymbolSearch("<=>")
        .addTypeParameter(symbolType)
        .setOfTypeOrReturn(symbolAndScopeManagement.getEk9Types().ek9Integer());
  }
}

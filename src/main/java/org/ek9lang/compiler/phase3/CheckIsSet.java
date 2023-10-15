package org.ek9lang.compiler.phase3;

import java.util.function.BiPredicate;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.symbols.FunctionSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * Given a symbol, this check that the type that the symbol has can support the '?' operation.
 * This includes variable/expressions that have a type that is a function.
 */
class CheckIsSet extends OperatorCheck implements BiPredicate<IToken, ISymbol> {

  CheckIsSet(final SymbolAndScopeManagement symbolAndScopeManagement,
             final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
  }

  @Override
  public boolean test(final IToken operatorUse, final ISymbol symbol) {
    var symbolType = getSymbolType(symbol);
    if (symbolType.isEmpty()) {
      return false;
    }

    if (symbolType.get() instanceof FunctionSymbol) {
      return true;
    }

    var search = getMethodSymbolSearch(symbolType.get());
    return isOperatorPresent(new CheckOperatorData(symbol, operatorUse, search));
  }

  @Override
  protected MethodSymbolSearch getMethodSymbolSearch(final ISymbol symbolType) {
    return new MethodSymbolSearch("?")
        .setOfTypeOrReturn(symbolAndScopeManagement.getEk9Types().ek9Boolean());
  }
}

package org.ek9lang.compiler.phase3;

import java.util.function.BiPredicate;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.symbols.FunctionSymbol;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * Given a symbol, this check that the type that the symbol has can support the '?' operation.
 * This includes variable/expressions that have a type that is a function.
 */
final class CheckIsSet extends OperatorCheck implements BiPredicate<IToken, ISymbol> {

  CheckIsSet(final SymbolAndScopeManagement symbolAndScopeManagement,
             final ErrorListener errorListener) {

    super(symbolAndScopeManagement, errorListener);

  }

  @Override
  public boolean test(final IToken operatorUse, final ISymbol symbol) {

    final var symbolType = getSymbolType(symbol);

    if (symbolType.isEmpty()) {
      return false;
    }

    if (symbolType.get() instanceof FunctionSymbol) {
      return true;
    }

    //Also cater for an aggregate type that is 'text', it does not have operators but can be checked with
    //? and :=? operators and so when doing an implementation it will just be a null pointer check.
    //Clearly when we come to do the code generation - we will check if the '?' does actually exist because it may not.
    if (symbolType.get() instanceof IAggregateSymbol aggregate
        && (aggregate.getGenus() == ISymbol.SymbolGenus.TEXT
        || aggregate.getGenus() == ISymbol.SymbolGenus.TEXT_BASE)) {
      return true;
    }

    final var search = getMethodSymbolSearch(symbolType.get());

    return isOperatorPresent(new CheckOperatorData(symbol, operatorUse, search));
  }

  @Override
  protected MethodSymbolSearch getMethodSymbolSearch(final ISymbol symbolType) {

    return new MethodSymbolSearch("?")
        .setOfTypeOrReturn(symbolAndScopeManagement.getEk9Types().ek9Boolean());
  }
}

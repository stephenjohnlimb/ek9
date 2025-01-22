package org.ek9lang.compiler.phase3;

import java.util.function.BiPredicate;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.symbols.FunctionSymbol;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.SymbolCategory;
import org.ek9lang.compiler.symbols.SymbolGenus;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * Given a symbol, this check that the type that the symbol has can support the '?' operation.
 * This includes variable/expressions that have a type that is a function.
 */
final class IsSetPresentOrError extends OperatorTest implements BiPredicate<IToken, ISymbol> {

  IsSetPresentOrError(final SymbolsAndScopes symbolsAndScopes,
                      final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);

  }

  @Override
  public boolean test(final IToken operatorUse, final ISymbol symbol) {

    final var possibleSymbolType = getSymbolType(symbol);

    if (possibleSymbolType.isEmpty()) {
      return false;
    }

    final var symbolType = possibleSymbolType.get();
    //Functions and 'Any' always supported for isSet ad functions/return true (if not null delegates)
    //'Any' will actually have a sub-type and that may implement is-set and that would be used, else it returns a
    //default value.
    if (symbolType instanceof FunctionSymbol
        || symbolType.getCategory().equals(SymbolCategory.ANY)) {
      return true;
    }

    //Also cater for an aggregate type that is 'text', it does not have operators but can be checked with
    //? and :=? operators and so when doing an implementation it will just be a null pointer check.
    //Clearly when we come to do the code generation - we will check if the '?' does actually exist because it may not.
    if (symbolType instanceof IAggregateSymbol aggregate
        && (aggregate.getGenus() == SymbolGenus.TEXT
        || aggregate.getGenus() == SymbolGenus.TEXT_BASE)) {
      return true;
    }

    final var search = getMethodSymbolSearch(symbolType);

    return isOperatorPresent(new CheckOperatorData(symbol, operatorUse, search));
  }

  @Override
  protected MethodSymbolSearch getMethodSymbolSearch(final ISymbol symbolType) {

    return new MethodSymbolSearch("?")
        .setOfTypeOrReturn(symbolsAndScopes.getEk9Types().ek9Boolean());
  }
}

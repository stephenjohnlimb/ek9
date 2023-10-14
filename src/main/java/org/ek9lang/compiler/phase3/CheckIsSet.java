package org.ek9lang.compiler.phase3;

import java.util.function.BiPredicate;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.symbols.FunctionSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * Given a symbol, this check that the type that the symbol has can support the '?' operation.
 * This includes variable/expressions that have a type that is a function.
 */
class CheckIsSet extends RuleSupport implements BiPredicate<IToken, ISymbol> {
  private final SymbolTypeOrError symbolTypeOrError;
  private final CheckInitialised checkInitialised;
  private final CheckForOperator checkForOperator;

  CheckIsSet(final SymbolAndScopeManagement symbolAndScopeManagement,
             final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
    this.symbolTypeOrError = new SymbolTypeOrError(symbolAndScopeManagement, errorListener);
    this.checkInitialised = new CheckInitialised(symbolAndScopeManagement, errorListener);
    this.checkForOperator = new CheckForOperator(symbolAndScopeManagement, errorListener);
  }

  @Override
  public boolean test(final IToken operatorUse, final ISymbol symbol) {
    checkInitialised.accept(symbol);

    //Get the underlying type or emit error and return false.
    var symbolType = symbolTypeOrError.apply(symbol);

    if (symbolType.isEmpty()) {
      return false;
    }

    if (symbolType.get() instanceof FunctionSymbol) {
      return true;
    }

    var search = new MethodSymbolSearch("?")
        .setOfTypeOrReturn(symbolAndScopeManagement.getEk9Types().ek9Boolean());
    var locatedIsSet = checkForOperator.apply(new CheckOperatorData(symbol, operatorUse, search));
    return locatedIsSet.isPresent();
  }
}

package org.ek9lang.compiler.phase3;

import java.util.Optional;
import java.util.function.BiPredicate;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * Provides a common set of operations for checking operators.
 */
abstract class OperatorCheck extends TypedSymbolAccess implements BiPredicate<IToken, ISymbol> {
  private final SymbolTypeOrEmpty symbolTypeOrEmpty = new SymbolTypeOrEmpty();

  private final CheckForOperator checkForOperator;

  protected OperatorCheck(SymbolAndScopeManagement symbolAndScopeManagement,
                          ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
    this.checkForOperator = new CheckForOperator(symbolAndScopeManagement, errorListener);
  }

  protected abstract MethodSymbolSearch getMethodSymbolSearch(final ISymbol symbolType);

  protected Optional<ISymbol> getSymbolType(final ISymbol symbol) {

    //Get the underlying type or emit error and return false.
    var symbolType = symbolTypeOrEmpty.apply(symbol);

    //Need an ISymbol here to return not a PossibleGenericSymbol
    if (symbolType.isPresent()) {
      var value = symbolType.get();
      return Optional.of(value);
    }

    return Optional.empty();
  }

  protected boolean isOperatorPresent(final CheckOperatorData data) {
    var locatedIsSet = checkForOperator.apply(data);
    return locatedIsSet.isPresent();
  }

  @Override
  public boolean test(final IToken operatorUse, final ISymbol symbol) {
    var symbolType = getSymbolType(symbol);
    if (symbolType.isEmpty()) {
      return false;
    }
    var search = getMethodSymbolSearch(symbolType.get());
    return isOperatorPresent(new CheckOperatorData(symbol, operatorUse, search));
  }
}

package org.ek9lang.compiler.phase3;

import java.util.Optional;
import java.util.function.BiPredicate;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * Provides a common set of operations for checking operators.
 */
abstract class OperatorCheck extends TypedSymbolAccess implements BiPredicate<IToken, ISymbol> {
  private final SymbolTypeOrEmpty symbolTypeOrEmpty = new SymbolTypeOrEmpty();

  private final CheckForOperator checkForOperator;

  protected OperatorCheck(final SymbolsAndScopes symbolsAndScopes,
                          final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);
    this.checkForOperator = new CheckForOperator(symbolsAndScopes, errorListener);

  }

  protected abstract MethodSymbolSearch getMethodSymbolSearch(final ISymbol symbolType);

  protected Optional<ISymbol> getSymbolType(final ISymbol symbol) {

    //Get the underlying type or emit error and return false.
    final var symbolType = symbolTypeOrEmpty.apply(symbol);

    //Need an ISymbol here to return not a PossibleGenericSymbol
    if (symbolType.isPresent()) {
      final var value = symbolType.get();
      return Optional.of(value);
    }

    return Optional.empty();
  }

  protected boolean isOperatorPresent(final CheckOperatorData data) {

    final var locatedIsSet = checkForOperator.apply(data);

    return locatedIsSet.isPresent();
  }

  @Override
  public boolean test(final IToken operatorUse, final ISymbol symbol) {

    final var symbolType = getSymbolType(symbol);
    if (symbolType.isPresent()) {
      final var search = getMethodSymbolSearch(symbolType.get());
      return isOperatorPresent(new CheckOperatorData(symbol, operatorUse, search));
    }

    return false;
  }
}

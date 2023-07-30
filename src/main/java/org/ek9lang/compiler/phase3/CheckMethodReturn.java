package org.ek9lang.compiler.phase3;

import java.util.List;
import java.util.function.Consumer;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.support.RuleSupport;
import org.ek9lang.compiler.support.SymbolAndScopeManagement;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;

/**
 * Checks that non-abstract methods that do have a return, actually do initialise that return.
 */
final class CheckMethodReturn extends RuleSupport implements Consumer<AggregateSymbol> {
  private final CheckReturn checkReturn;

  CheckMethodReturn(final SymbolAndScopeManagement symbolAndScopeManagement,
                    final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
    this.checkReturn = new CheckReturn(false, symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(AggregateSymbol aggregateSymbol) {
    List<MethodSymbol> nonAbstractMethodsToCheck = aggregateSymbol.getAllNonAbstractMethodsInThisScopeOnly();
    nonAbstractMethodsToCheck.forEach(
        methodSymbol -> checkReturn.accept(methodSymbol, methodSymbol.getReturningSymbol()));
  }
}

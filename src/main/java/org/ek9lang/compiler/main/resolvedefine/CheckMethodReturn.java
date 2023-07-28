package org.ek9lang.compiler.main.resolvedefine;

import java.util.List;
import java.util.function.Consumer;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.main.phases.definition.SymbolAndScopeManagement;
import org.ek9lang.compiler.support.RuleSupport;
import org.ek9lang.compiler.symbol.AggregateSymbol;
import org.ek9lang.compiler.symbol.MethodSymbol;

/**
 * Checks that non-abstract methods that do have a return, actually do initialise that return.
 */
public class CheckMethodReturn extends RuleSupport implements Consumer<AggregateSymbol> {
  private final CheckReturn checkReturn;

  public CheckMethodReturn(final SymbolAndScopeManagement symbolAndScopeManagement,
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

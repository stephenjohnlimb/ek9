package org.ek9lang.compiler.phase1;

import static org.ek9lang.compiler.support.AggregateFactory.EK9_VOID;

import java.util.function.Consumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.search.TypeSymbolSearch;
import org.ek9lang.compiler.support.SymbolFactory;
import org.ek9lang.compiler.symbols.IMayReturnSymbol;
import org.ek9lang.compiler.symbols.ScopedSymbol;

class ProcessSyntheticReturn extends RuleSupport implements Consumer<IMayReturnSymbol> {

  private final SymbolFactory symbolFactory;

  protected ProcessSyntheticReturn(final SymbolAndScopeManagement symbolAndScopeManagement,
                                   final SymbolFactory symbolFactory,
                                   final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
    this.symbolFactory = symbolFactory;
  }

  @Override
  public void accept(IMayReturnSymbol symbol) {
    if (symbol instanceof ScopedSymbol scopedSymbol) {
      final var simulatedVoid = symbolFactory.newVariable("_rtn", scopedSymbol.getSourceToken(), false, false);
      if (symbolAndScopeManagement.getEk9Types() != null) {
        simulatedVoid.setType(symbolAndScopeManagement.getEk9Types().ek9Void());
      } else {
        simulatedVoid.setType(scopedSymbol.resolve(new TypeSymbolSearch(EK9_VOID)));
      }
      symbol.setReturningSymbol(simulatedVoid);
    }
  }
}

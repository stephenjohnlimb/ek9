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

/**
 * Constructs like methods and functions may or may not always retun a value.
 * So when defining these in EK9 the developer can decide not to return any value.
 * However within the compiler when resolving expressions and calls, it is important to
 * fill in the 'void' definition for the returning type.
 * That is what this component does. In the case where no return value is defined by the EK9 developer
 * the compiler creates a dummy '_rtn' variable and gives it the type Void.
 * So when the compiler comes to check expression use it can see that a function/method does not actually
 * return anything and therefore it can emit errors (in the case where the EK9 developer is incorrectly expecting
 * some form of value).
 */
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

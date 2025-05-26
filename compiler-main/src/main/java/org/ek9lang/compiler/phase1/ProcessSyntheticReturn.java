package org.ek9lang.compiler.phase1;

import static org.ek9lang.compiler.support.AggregateManipulator.EK9_VOID;

import java.util.function.Consumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.search.TypeSymbolSearch;
import org.ek9lang.compiler.support.SymbolFactory;
import org.ek9lang.compiler.symbols.IMayReturnSymbol;
import org.ek9lang.compiler.symbols.ScopedSymbol;

/**
 * Constructs like methods and functions may or may not always return a value.
 * So when defining these in EK9 the developer can decide not to return any value.
 * However, within the compiler when resolving expressions and calls, it is important to
 * fill in the 'Void' definition for the returning type.
 * That is what this component does. In the case where no return value is defined by the EK9 developer
 * the compiler creates a dummy '_rtn' variable and gives it the type Void.
 * So when the compiler comes to check expression use it can see that a function/method does not actually
 * return anything and, therefore it can emit errors (in the case where the EK9 developer is incorrectly expecting
 * some form of value).
 */
class ProcessSyntheticReturn extends RuleSupport implements Consumer<IMayReturnSymbol> {

  private final SymbolFactory symbolFactory;

  protected ProcessSyntheticReturn(final SymbolsAndScopes symbolsAndScopes,
                                   final SymbolFactory symbolFactory,
                                   final ErrorListener errorListener) {
    super(symbolsAndScopes, errorListener);
    this.symbolFactory = symbolFactory;
  }

  @Override
  public void accept(IMayReturnSymbol symbol) {
    if (symbol instanceof ScopedSymbol scopedSymbol) {
      final var simulatedVoid = symbolFactory.newVariable("_rtn", scopedSymbol.getSourceToken(), false, false);
      if (symbolsAndScopes.getEk9Types() != null) {
        simulatedVoid.setType(symbolsAndScopes.getEk9Types().ek9Void());
      } else {
        simulatedVoid.setType(scopedSymbol.resolve(new TypeSymbolSearch(EK9_VOID)));
      }
      symbol.setReturningSymbol(simulatedVoid);
    }
  }
}

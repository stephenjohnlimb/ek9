package org.ek9lang.compiler.common;

import static org.ek9lang.compiler.support.EK9TypeNames.EK9_VOID;

import java.util.function.BiConsumer;
import org.ek9lang.antlr.EK9Parser;
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
 * This won't add a synthetic return it is not needed.
 */
public class ProcessSyntheticReturn extends RuleSupport
    implements BiConsumer<EK9Parser.OperationDetailsContext, IMayReturnSymbol> {

  private final SymbolFactory symbolFactory;

  public ProcessSyntheticReturn(final SymbolsAndScopes symbolsAndScopes,
                                final SymbolFactory symbolFactory,
                                final ErrorListener errorListener) {
    super(symbolsAndScopes, errorListener);
    this.symbolFactory = symbolFactory;
  }

  @Override
  public void accept(final EK9Parser.OperationDetailsContext ctx, final IMayReturnSymbol symbol) {

    if (symbol.isReturningSymbolPresent() && symbol.getReturningSymbol().getType().isPresent()) {
      return;
    }

    //If there is no operation context, or if there is no returning, then we need a synthetic _rtn Void.
    if (ctx == null || ctx.returningParam() == null) {
      addSyntheticReturn(symbol);
    }
  }

  private void addSyntheticReturn(final IMayReturnSymbol symbol) {
    if (symbol instanceof ScopedSymbol scopedSymbol) {

      final var simulatedVoidRtn = symbolFactory.newVariable("_rtn", scopedSymbol.getSourceToken(), false, false);
      //Now the catch 22 - we might not have actually processed Void yet. In bootstrap it will be defined later in file
      if (symbolsAndScopes.getEk9Types() != null) {
        simulatedVoidRtn.setType(symbolsAndScopes.getEk9Types().ek9Void());
      } else {
        simulatedVoidRtn.setType(scopedSymbol.resolve(new TypeSymbolSearch(EK9_VOID)));
      }
      //So only if we resolve it do we set it.
      //This BiConsumer will be called in later phases (after definition phase).
      if (simulatedVoidRtn.getType().isPresent()) {
        symbol.setReturningSymbol(simulatedVoidRtn);
      }
    }
  }
}

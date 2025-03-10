package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.symbols.IFunctionSymbol;

/**
 * The ek9 developer does not need to redeclare incoming or returning parameters for dynamic functions.
 * They are inferred for the developer - this is that 'inference'.
 * In short the compiler - uses the same symbols from the super class for incoming and returning parameters.
 */
final class AutoMatchSuperFunctionSignature extends TypedSymbolAccess implements Consumer<IFunctionSymbol> {
  AutoMatchSuperFunctionSignature(final SymbolsAndScopes symbolsAndScopes,
                                  final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);

  }

  @Override
  public void accept(final IFunctionSymbol functionSymbol) {

    functionSymbol.getSuperFunction().ifPresent(superFunction -> {
      superFunction.getSymbolsForThisScope().forEach(param -> functionSymbol.define(param.clone(functionSymbol)));
      if (superFunction.isReturningSymbolPresent()) {
        final var clonedReturnSymbol = superFunction.getReturningSymbol().clone(functionSymbol);
        //Ensure not marked as initialised if the super is abstract, this then forces return processing.
        if (superFunction.isMarkedAbstract()) {
          clonedReturnSymbol.clearInitialisedBy();
        }
        functionSymbol.setReturningSymbol(clonedReturnSymbol);
      }
    });

  }
}

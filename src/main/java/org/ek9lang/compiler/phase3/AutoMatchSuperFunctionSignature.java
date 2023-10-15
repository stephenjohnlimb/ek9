package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.symbols.FunctionSymbol;

/**
 * The ek9 develop does not need to redeclare incoming or returning parameters for dynamic functions.
 * They are inferred for the developer - this is that 'inference'.
 * In short the compiler - uses the same symbols from the super class for incoming and returning parameters.
 */
final class AutoMatchSuperFunctionSignature extends TypedSymbolAccess implements Consumer<FunctionSymbol> {
  AutoMatchSuperFunctionSignature(final SymbolAndScopeManagement symbolAndScopeManagement,
                                  final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(FunctionSymbol functionSymbol) {
    functionSymbol.getSuperFunctionSymbol().ifPresent(superFunction -> {
      superFunction.getSymbolsForThisScope().forEach(param -> functionSymbol.define(param.clone(functionSymbol)));
      if (superFunction.isReturningSymbolPresent()) {
        var clonedReturnSymbol = superFunction.getReturningSymbol().clone(functionSymbol);
        //Ensure not marked as initialised if the super is abstract, this then forces return processing.
        if (superFunction.isMarkedAbstract()) {
          clonedReturnSymbol.setInitialisedBy(null);
        }
        functionSymbol.setReturningSymbol(clonedReturnSymbol);
      }
    });
  }
}

package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.symbols.FunctionSymbol;

/**
 * Checks if a function extends another function and if it is abstract.
 * Errors if this configuration does not make sense.
 */
final class ValidFunctionAbstractnessOrError extends TypedSymbolAccess implements Consumer<FunctionSymbol> {
  ValidFunctionAbstractnessOrError(final SymbolsAndScopes symbolsAndScopes,
                                   final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);

  }

  @Override
  public void accept(final FunctionSymbol functionSymbol) {

    //Only worth checking if abstract.
    if (functionSymbol.isMarkedAbstract()) {
      functionSymbol.getSuperFunction().ifPresent(superFunction -> {
        if (!superFunction.isMarkedAbstract()) {
          final var errorMessage = "pointless abstract function extending '" + superFunction.getFriendlyName() + "':";
          errorListener.semanticError(functionSymbol.getSourceToken(), errorMessage,
              ErrorListener.SemanticClassification.CANNOT_BE_ABSTRACT);
        }
      });
    }

  }
}

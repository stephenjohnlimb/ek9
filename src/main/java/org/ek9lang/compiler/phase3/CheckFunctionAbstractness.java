package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.symbols.FunctionSymbol;

/**
 * Checks if a function extends another function and if it is abstract.
 * Errors if this configuration does not make sense.
 */
final class CheckFunctionAbstractness extends RuleSupport implements Consumer<FunctionSymbol> {
  CheckFunctionAbstractness(final SymbolAndScopeManagement symbolAndScopeManagement,
                            final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(FunctionSymbol functionSymbol) {
    //Only worth checking if abstract.
    if (functionSymbol.isMarkedAbstract()) {
      functionSymbol.getSuperFunctionSymbol().ifPresent(superFunction -> {
        if (!superFunction.isMarkedAbstract()) {
          var errorMessage = "pointless abstract function extending '" + superFunction.getFriendlyName() + "':";
          errorListener.semanticError(functionSymbol.getSourceToken(), errorMessage,
              ErrorListener.SemanticClassification.CANNOT_BE_ABSTRACT);
        }
      });
    }
  }
}

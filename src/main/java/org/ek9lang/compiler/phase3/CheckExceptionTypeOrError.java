package org.ek9lang.compiler.phase3;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.TYPE_MUST_EXTEND_EXCEPTION;

import java.util.function.BiConsumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * Checks that the symbol passed has a type and that the type is compatible with an EK9 Exception.
 */
final class CheckExceptionTypeOrError extends TypedSymbolAccess implements BiConsumer<IToken, ISymbol> {
  CheckExceptionTypeOrError(SymbolAndScopeManagement symbolAndScopeManagement,
                            ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(final IToken errorLocation, final ISymbol symbol) {
    if (symbol != null) {
      symbol.getType().ifPresent(symbolType -> {
        if (!symbolType.isAssignableTo(symbolAndScopeManagement.getEk9Types().ek9Exception())) {
          errorListener.semanticError(errorLocation, "wrt '" + symbol.getFriendlyName() + "':",
              TYPE_MUST_EXTEND_EXCEPTION);
        }
      });
    }
  }
}

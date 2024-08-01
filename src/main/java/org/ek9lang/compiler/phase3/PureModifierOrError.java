package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;

/**
 * Checks if the pure modifier has been used correctly on methods and functions.
 */
final class PureModifierOrError extends TypedSymbolAccess implements Consumer<PureCheckData> {
  PureModifierOrError(final SymbolsAndScopes symbolsAndScopes,
                      final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);

  }

  @Override
  public void accept(final PureCheckData data) {

    if (data.superSymbol().isMarkedPure() && !data.thisSymbol().isMarkedPure()) {
      errorListener.semanticError(data.thisSymbol().getSourceToken(), data.errorMessage(),
          ErrorListener.SemanticClassification.SUPER_IS_PURE);
    } else if (!data.superSymbol().isMarkedPure() && data.thisSymbol().isMarkedPure()) {
      errorListener.semanticError(data.thisSymbol().getSourceToken(), data.errorMessage(),
          ErrorListener.SemanticClassification.SUPER_IS_NOT_PURE);
    }
  }
}

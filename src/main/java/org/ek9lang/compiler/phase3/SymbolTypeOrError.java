package org.ek9lang.compiler.phase3;

import java.util.Optional;
import java.util.function.Function;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.PossibleGenericSymbol;

/**
 * Provides the PossibleGenericSymbol type of the symbol if it has been typed, or emits an error if not.
 * This is because the 'type' can be an aggregate or it can be a function.
 */
final class SymbolTypeOrError extends RuleSupport implements Function<ISymbol, Optional<PossibleGenericSymbol>> {
  SymbolTypeOrError(final SymbolAndScopeManagement symbolAndScopeManagement,
                    final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
  }

  @Override
  public Optional<PossibleGenericSymbol> apply(final ISymbol symbol) {
    if (symbol == null) {
      return Optional.empty();
    }

    if (symbol.getType().isPresent() && symbol.getType().get() instanceof PossibleGenericSymbol typeSymbol) {
      //Note that we have now had cause to reference this symbol in some way, so mark it as referenced.
      symbol.setReferenced(true);
      return Optional.of(typeSymbol);
    }
    emitTypeNotResolvedError(symbol);
    return Optional.empty();
  }

  private void emitTypeNotResolvedError(final ISymbol symbol) {
    var msg = "'" + symbol.getName() + "':";
    errorListener.semanticError(symbol.getSourceToken(), msg, ErrorListener.SemanticClassification.TYPE_NOT_RESOLVED);
  }
}

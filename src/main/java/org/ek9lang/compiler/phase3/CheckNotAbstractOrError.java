package org.ek9lang.compiler.phase3;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.CANNOT_CALL_ABSTRACT_TYPE;

import java.util.function.BiConsumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.support.LocationExtractorFromSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * Typically used when a direct call is being bad on the symbol.
 * So this would be a Function or an Aggregate (type).
 */
final class CheckNotAbstractOrError extends TypedSymbolAccess implements BiConsumer<IToken, ISymbol> {
  private final LocationExtractorFromSymbol locationExtractorFromSymbol = new LocationExtractorFromSymbol();

  CheckNotAbstractOrError(final SymbolAndScopeManagement symbolAndScopeManagement,
                          final ErrorListener errorListener) {

    super(symbolAndScopeManagement, errorListener);

  }

  @Override
  public void accept(final IToken token, final ISymbol symbol) {

    if (symbol.isMarkedAbstract()) {
      final var location = locationExtractorFromSymbol.apply(symbol);
      final var msg = "which is a call to '" + symbol.getFriendlyName() + "' " + location + ":";
      errorListener.semanticError(token, msg, CANNOT_CALL_ABSTRACT_TYPE);
    }
    
  }
}

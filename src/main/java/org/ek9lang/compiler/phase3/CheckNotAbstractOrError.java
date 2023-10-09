package org.ek9lang.compiler.phase3;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.CANNOT_CALL_ABSTRACT_TYPE;

import java.util.function.BiConsumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.support.LocationExtractor;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * Typically used when a direct call is being bad on the symbol.
 * So this would be a Function or an Aggregate (type).
 */
public class CheckNotAbstractOrError extends RuleSupport implements BiConsumer<IToken, ISymbol> {
  private final LocationExtractor locationExtractor = new LocationExtractor();

  protected CheckNotAbstractOrError(SymbolAndScopeManagement symbolAndScopeManagement,
                                    ErrorListener errorListener) {

    super(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(final IToken token, final ISymbol symbol) {
    if (symbol.isMarkedAbstract()) {
      var location = locationExtractor.apply(symbol);
      var msg = "which is a call to '" + symbol.getFriendlyName() + "' " + location + ":";
      errorListener.semanticError(token, msg, CANNOT_CALL_ABSTRACT_TYPE);
    }
  }
}

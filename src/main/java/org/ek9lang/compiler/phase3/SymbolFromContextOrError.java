package org.ek9lang.compiler.phase3;

import java.util.function.Function;
import org.antlr.v4.runtime.ParserRuleContext;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Returns the appropriate symbol from the symbol and scope management component.
 * But if it is null issues a semantic error.
 * Not this is not looking up through scopes or anything, just looking directly as what symbol
 * (if any) has been recorded against the context passed in.
 */
final class SymbolFromContextOrError extends TypedSymbolAccess implements Function<ParserRuleContext, ISymbol> {

  SymbolFromContextOrError(final SymbolsAndScopes symbolsAndScopes,
                           final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);

  }

  @Override
  public ISymbol apply(final ParserRuleContext ctx) {

    var resolved = getRecordedAndTypedSymbol(ctx);
    if (resolved == null) {
      errorListener.semanticError(ctx.start, "", ErrorListener.SemanticClassification.NOT_RESOLVED);
    }

    return resolved;
  }
}

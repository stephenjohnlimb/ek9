package org.ek9lang.compiler.main.resolvedefine;

import java.util.function.Function;
import org.antlr.v4.runtime.ParserRuleContext;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.main.phases.definition.SymbolAndScopeManagement;
import org.ek9lang.compiler.symbol.ISymbol;

/**
 * Returns the appropriate symbol from the symbol and scope management component.
 * But if it is null issues a semantic error.
 * Not this is not looking up through scopes or anything, just looking directly as what symbol
 * (if any) has been recorded against the context passed in.
 */
public class SymbolFromContextOrError implements Function<ParserRuleContext, ISymbol> {
  private final SymbolAndScopeManagement symbolAndScopeManagement;

  private final ErrorListener errorListener;

  public SymbolFromContextOrError(final SymbolAndScopeManagement symbolAndScopeManagement,
                                  final ErrorListener errorListener) {
    this.symbolAndScopeManagement = symbolAndScopeManagement;
    this.errorListener = errorListener;
  }

  @Override
  public ISymbol apply(ParserRuleContext ctx) {
    var resolved = symbolAndScopeManagement.getRecordedSymbol(ctx);
    if (resolved == null) {
      errorListener.semanticError(ctx.start, "", ErrorListener.SemanticClassification.NOT_RESOLVED);
    }
    return resolved;
  }
}

package org.ek9lang.compiler.phase3;

import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.search.SymbolSearch;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Used for pure identifier resolution and recording. Rather than identifierReferences.
 */
final class ResolveIdentifierOrError extends TypedSymbolAccess
    implements Function<EK9Parser.IdentifierContext, ISymbol> {

  /**
   * Searches for an identifier and issues an error if it is not resolved.
   * So identifiers are a little ambiguous unlike identifierReferences (which must be present).
   * identifiers can be optional in some contexts (like named argument when calling methods and functions).
   * Also in dynamic classes and functions with variable capture.
   */
  ResolveIdentifierOrError(final SymbolAndScopeManagement symbolAndScopeManagement,
                           final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
  }

  @Override
  public ISymbol apply(final EK9Parser.IdentifierContext ctx) {

    var toResolve = ctx.getText();
    var resolved = symbolAndScopeManagement.getTopScope().resolve(new SymbolSearch(toResolve));
    if (resolved.isEmpty()) {
      var msg = "'" + toResolve + "':";
      errorListener.semanticError(ctx.start, msg, ErrorListener.SemanticClassification.NOT_RESOLVED);
      return null;
    }
    var identifierSymbol = resolved.get();
    identifierSymbol.setReferenced(true);
    recordATypedSymbol(identifierSymbol, ctx);
    return identifierSymbol;
  }
}
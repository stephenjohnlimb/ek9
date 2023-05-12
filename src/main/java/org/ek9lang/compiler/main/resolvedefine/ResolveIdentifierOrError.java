package org.ek9lang.compiler.main.resolvedefine;

import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.main.phases.definition.SymbolAndScopeManagement;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.support.search.SymbolSearch;

/**
 * Used for pure identifier resolution and recording. Rather than identifierReferences.
 */
public class ResolveIdentifierOrError implements Function<EK9Parser.IdentifierContext, ISymbol> {

  private final SymbolAndScopeManagement symbolAndScopeManagement;

  private final ErrorListener errorListener;

  /**
   * Searches for an identifier and issues an error if it is not resolved.
   * So identifiers are a little ambiguous unlike identifierReferences (which must be present).
   * identifiers can be optional in some contexts (like named argument when calling methods and functions).
   * Also in dynamic classes and functions with variable capture.
   */
  public ResolveIdentifierOrError(final SymbolAndScopeManagement symbolAndScopeManagement,
                                  final ErrorListener errorListener) {
    this.symbolAndScopeManagement = symbolAndScopeManagement;
    this.errorListener = errorListener;
  }

  @Override
  public ISymbol apply(EK9Parser.IdentifierContext ctx) {

    var toResolve = ctx.getText();
    var resolved = symbolAndScopeManagement.getTopScope().resolve(new SymbolSearch(toResolve));
    if (resolved.isEmpty()) {
      var msg = "'" + toResolve + "':";
      errorListener.semanticError(ctx.start, msg, ErrorListener.SemanticClassification.NOT_RESOLVED);
      return null;
    }
    var identifierSymbol = resolved.get();
    identifierSymbol.setReferenced(true);
    symbolAndScopeManagement.recordSymbol(identifierSymbol, ctx);
    return identifierSymbol;
  }
}
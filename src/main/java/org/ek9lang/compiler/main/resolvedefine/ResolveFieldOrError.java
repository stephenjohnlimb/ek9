package org.ek9lang.compiler.main.resolvedefine;

import java.util.function.BiFunction;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.main.phases.definition.SymbolAndScopeManagement;
import org.ek9lang.compiler.symbol.IScope;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.support.search.SymbolSearch;

/**
 * Used for resolving and recoding of fields on specific types.
 * This takes into account field visibility and where access is being made from.
 * So it will work on a range of aggregates, records, classes, components, etc.
 */
public class ResolveFieldOrError implements BiFunction<EK9Parser.IdentifierContext, IScope, ISymbol> {

  private final SymbolAndScopeManagement symbolAndScopeManagement;

  private final ErrorListener errorListener;
  private final MostSpecificScope mostSpecificScope;

  private final CheckAccessToSymbol checkAccessToSymbol;

  /**
   * Create a new field resolver.
   */
  public ResolveFieldOrError(final SymbolAndScopeManagement symbolAndScopeManagement,
                             final ErrorListener errorListener) {
    this.symbolAndScopeManagement = symbolAndScopeManagement;
    this.errorListener = errorListener;

    this.mostSpecificScope = new MostSpecificScope(symbolAndScopeManagement);
    this.checkAccessToSymbol = new CheckAccessToSymbol(errorListener);
  }

  @Override
  public ISymbol apply(final EK9Parser.IdentifierContext ctx, final IScope scopeToResolveIn) {

    var identifier = ctx.getText();
    var accessFromScope = mostSpecificScope.get();
    final var errorMsgBase = "'" + identifier + "' on '" + scopeToResolveIn.getFriendlyScopeName() + "':";
    var resolved = scopeToResolveIn.resolveMember(new SymbolSearch(identifier));
    if (resolved.isEmpty()) {
      errorListener.semanticError(ctx.start, errorMsgBase, ErrorListener.SemanticClassification.NOT_RESOLVED);
      return null;
    }

    var identifierSymbol = resolved.get();
    var checkingData =
        new CheckSymbolAccessData(ctx.start, accessFromScope, scopeToResolveIn, identifier, identifierSymbol);

    checkAccessToSymbol.accept(checkingData);

    identifierSymbol.setReferenced(true);
    symbolAndScopeManagement.recordSymbol(identifierSymbol, ctx);
    return identifierSymbol;
  }
}
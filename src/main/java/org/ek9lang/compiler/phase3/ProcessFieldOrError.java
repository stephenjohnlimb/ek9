package org.ek9lang.compiler.phase3;

import java.util.function.BiFunction;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.search.SymbolSearch;
import org.ek9lang.compiler.support.MostSpecificScope;
import org.ek9lang.compiler.symbols.IScope;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.tokenizer.Ek9Token;

/**
 * Used for resolving and recoding of fields on specific types.
 * This takes into account field visibility and where access is being made from.
 * So it will work on a range of aggregates, records, classes, components, etc.
 */
final class ProcessFieldOrError extends TypedSymbolAccess
    implements BiFunction<EK9Parser.IdentifierContext, IScope, ISymbol> {
  private final MostSpecificScope mostSpecificScope;
  private final CheckAccessToSymbol checkAccessToSymbol;

  /**
   * Create a new field resolver.
   */
  ProcessFieldOrError(final SymbolAndScopeManagement symbolAndScopeManagement,
                      final ErrorListener errorListener) {

    super(symbolAndScopeManagement, errorListener);

    this.mostSpecificScope = new MostSpecificScope(symbolAndScopeManagement);
    this.checkAccessToSymbol = new CheckAccessToSymbol(symbolAndScopeManagement, errorListener);

  }

  @Override
  public ISymbol apply(final EK9Parser.IdentifierContext ctx, final IScope scopeToResolveIn) {

    final var identifier = ctx.getText();
    final var resolved = scopeToResolveIn.resolveMember(new SymbolSearch(identifier));

    if (resolved.isPresent()) {
      final var identifierSymbol = resolved.get();
      final var accessFromScope = mostSpecificScope.get();
      final var checkingData = new CheckSymbolAccessData(new Ek9Token(ctx.start),
          accessFromScope, scopeToResolveIn, identifier, identifierSymbol);

      checkAccessToSymbol.accept(checkingData);
      identifierSymbol.setReferenced(true);
      recordATypedSymbol(identifierSymbol, ctx);

      return identifierSymbol;
    }

    final var errorMsgBase = "'" + identifier + "' on '" + scopeToResolveIn.getFriendlyScopeName() + "':";
    errorListener.semanticError(ctx.start, errorMsgBase, ErrorListener.SemanticClassification.NOT_RESOLVED);

    return null;
  }
}
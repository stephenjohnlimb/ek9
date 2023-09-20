package org.ek9lang.compiler.phase3;

import java.util.function.BiFunction;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.search.MethodSearchInScope;
import org.ek9lang.compiler.search.MethodSymbolSearchResult;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * Given a search for a method from an aggregate - and include supers/traits etc,
 * this function will try and locate the method. But if not found or ambiguous it will issue errors.
 */
final class ResolveMethodOrError extends RuleSupport implements BiFunction<IToken, MethodSearchInScope, MethodSymbol> {

  private final MostSpecificScope mostSpecificScope;
  private final CheckAccessToSymbol checkAccessToSymbol;

  /**
   * Create function with provided errorListener etc.
   */
  ResolveMethodOrError(final SymbolAndScopeManagement symbolAndScopeManagement,
                       final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
    this.mostSpecificScope = new MostSpecificScope(symbolAndScopeManagement, errorListener);
    this.checkAccessToSymbol = new CheckAccessToSymbol(symbolAndScopeManagement, errorListener);
  }

  @Override
  public MethodSymbol apply(final IToken token, final MethodSearchInScope searchOnAggregate) {

    var accessFromScope = mostSpecificScope.get();

    var msgStart = "In relation to type '" + searchOnAggregate.scopeToSearch().getFriendlyScopeName() + "', and ";
    var results = searchOnAggregate.scopeToSearch()
        .resolveMatchingMethods(searchOnAggregate.search(), new MethodSymbolSearchResult());
    if (results.isSingleBestMatchPresent() && results.getSingleBestMatchSymbol().isPresent()) {
      var resolved = results.getSingleBestMatchSymbol().get();
      checkAccessToSymbol.accept(new CheckSymbolAccessData(token, accessFromScope, searchOnAggregate.scopeToSearch(),
          searchOnAggregate.search().getName(), resolved));
      return resolved;
    } else if (results.isAmbiguous()) {
      var msg = msgStart + "'"
          + searchOnAggregate.search().toString()
          + "' resolved: "
          + results.getAmbiguousMethodParameters();
      errorListener.semanticError(token, msg, ErrorListener.SemanticClassification.METHOD_AMBIGUOUS);
    } else if (results.isEmpty()) {
      var msg = msgStart + "'" + searchOnAggregate.search().toString() + "':";
      errorListener.semanticError(token, msg, ErrorListener.SemanticClassification.METHOD_NOT_RESOLVED);
    }
    return null;
  }
}

package org.ek9lang.compiler.phase3;

import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.search.MethodSearchInScope;
import org.ek9lang.compiler.search.MethodSymbolSearchResult;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * Given a search for a method from an aggregate - and include supers/traits etc,
 * this function will try and locate the method. But if not found or ambiguous it will issue errors.
 */
final class ResolveMethodOrError extends TypedSymbolAccess
    implements BiFunction<IToken, MethodSearchInScope, MethodSymbol> {
  private final MostSpecificScope mostSpecificScope;
  private final CheckAccessToSymbol checkAccessToSymbol;

  /**
   * Create function with provided errorListener etc.
   */
  ResolveMethodOrError(final SymbolAndScopeManagement symbolAndScopeManagement,
                       final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
    this.mostSpecificScope = new MostSpecificScope(symbolAndScopeManagement);
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
      var msg = msgStart + "'" + searchOnAggregate.search().toString();
      var nearMatches = getPossibleMatchingMethods(searchOnAggregate);
      if (nearMatches.isEmpty()) {
        msg += "':";
      } else {
        msg += "', parameter mismatch. Possible method(s) ";
        msg += methodsToPresentation(nearMatches);
        msg += ":";
      }
      errorListener.semanticError(token, msg, ErrorListener.SemanticClassification.METHOD_NOT_RESOLVED);
    }
    return null;
  }

  private String methodsToPresentation(List<MethodSymbol> methods) {
    return methods.stream().map(ISymbol::getFriendlyName).collect(Collectors.joining(","));
  }

  private List<MethodSymbol> getPossibleMatchingMethods(final MethodSearchInScope searchOnAggregate) {
    var toSearch = searchOnAggregate.scopeToSearch();
    var nearMatches = toSearch.getAllSymbolsMatchingName(searchOnAggregate.search().getName());
    return nearMatches
        .stream()
        .filter(ISymbol::isMethod)
        .map(MethodSymbol.class::cast)
        .toList();
  }
}

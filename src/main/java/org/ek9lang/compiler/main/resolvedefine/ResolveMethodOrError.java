package org.ek9lang.compiler.main.resolvedefine;

import java.util.function.BiFunction;
import org.antlr.v4.runtime.Token;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.symbol.MethodSymbol;
import org.ek9lang.compiler.symbol.support.search.MethodSearchOnAggregate;
import org.ek9lang.compiler.symbol.support.search.MethodSymbolSearchResult;

/**
 * Given a search for a method on an aggregate, this function will try and locate the method.
 * But if not found or ambiguous it will issue errors.
 */
public class ResolveMethodOrError implements BiFunction<Token, MethodSearchOnAggregate, MethodSymbol> {

  private final ErrorListener errorListener;

  /**
   * Create function with provided errorListener etc.
   */
  public ResolveMethodOrError(final ErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  @Override
  public MethodSymbol apply(final Token token, final MethodSearchOnAggregate searchOnAggregate) {
    var results = searchOnAggregate.aggregate()
        .resolveMatchingMethods(searchOnAggregate.search(), new MethodSymbolSearchResult());
    if (results.isSingleBestMatchPresent() && results.getSingleBestMatchSymbol().isPresent()) {
      return results.getSingleBestMatchSymbol().get();
    } else if (results.isAmbiguous()) {
      var msg = "Method '"
          + searchOnAggregate.search().toString()
          + "' resolved: "
          + results.getAmbiguousMethodParameters();
      errorListener.semanticError(token, msg, ErrorListener.SemanticClassification.METHOD_AMBIGUOUS);
    } else if (results.isEmpty()) {
      var msg = "Method '" + searchOnAggregate.search().toString() + "'";
      errorListener.semanticError(token, msg, ErrorListener.SemanticClassification.METHOD_NOT_RESOLVED);
    }
    return null;
  }
}

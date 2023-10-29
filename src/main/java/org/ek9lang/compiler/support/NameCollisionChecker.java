package org.ek9lang.compiler.support;

import java.util.Map;
import java.util.Optional;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.search.SymbolSearch;
import org.ek9lang.compiler.symbols.IScope;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.core.AssertValue;

/**
 * Designed to check if variable name and method names collide with Type/Function names via the same scope.
 */
public class NameCollisionChecker {

  private final ErrorListener errorListener;

  private final LocationExtractor locationExtractor = new LocationExtractor();

  public NameCollisionChecker(ErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  /**
   * Check for exising symbol in the scope.
   * if returns true then errors will have been added to the error listener.
   */
  public boolean errorsWhenNamesCollide(final IScope inScope, final ISymbol symbol) {
    AssertValue.checkNotNull("Scope cannot be null", inScope);
    AssertValue.checkNotNull("Symbol cannot be null", symbol);

    var searches = Map
        .of(ISymbol.SymbolCategory.FUNCTION, ErrorListener.SemanticClassification.DUPLICATE_NAME,
            ISymbol.SymbolCategory.TYPE, ErrorListener.SemanticClassification.DUPLICATE_TYPE,
            ISymbol.SymbolCategory.TEMPLATE_FUNCTION, ErrorListener.SemanticClassification.DUPLICATE_NAME,
            ISymbol.SymbolCategory.TEMPLATE_TYPE, ErrorListener.SemanticClassification.DUPLICATE_TYPE
        );

    //Not that we also stop same name as the types above.
    for (var entry : searches.entrySet()) {
      var search = new SymbolSearch(symbol.getFullyQualifiedName()).setSearchType(entry.getKey());
      if (errorsIfResolved(inScope, symbol, search, entry.getValue())) {
        return true;
      }
    }
    return false;
  }

  /**
   * If a symbol is resolved then this will emit an error.
   */
  public boolean errorsIfResolved(final IScope inScope, final ISymbol symbol, final SymbolSearch search,
                                  final ErrorListener.SemanticClassification classificationError) {

    Optional<ISymbol> symbolCheck = inScope.resolve(search);
    if (symbolCheck.isPresent()) {
      ISymbol dup = symbolCheck.get();
      String message = String.format("'%s' as %s %s:",
          dup.getFriendlyName(), dup.getGenus(), locationExtractor.apply(dup));
      errorListener.semanticError(symbol.getSourceToken(), message, classificationError);
      return true;
    }
    return false;
  }
}

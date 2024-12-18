package org.ek9lang.compiler.support;

import java.util.Map;
import java.util.function.BiPredicate;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.search.SymbolSearch;
import org.ek9lang.compiler.symbols.IScope;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.SymbolCategory;
import org.ek9lang.core.AssertValue;

/**
 * Designed to check if variable name and method names collide with Type/Function names.
 */
public class NoNameCollisionOrError implements BiPredicate<IScope, ISymbol> {

  private final boolean useQualifiedName;
  private final ErrorListener errorListener;

  private final LocationExtractorFromSymbol locationExtractorFromSymbol = new LocationExtractorFromSymbol();

  public NoNameCollisionOrError(final ErrorListener errorListener, final boolean useQualifiedName) {

    this.errorListener = errorListener;
    this.useQualifiedName = useQualifiedName;

  }

  /**
   * Check for exising symbol in the scope.
   * if returns true then errors will have been added to the error listener.
   */
  @Override
  public boolean test(final IScope inScope, final ISymbol symbol) {

    AssertValue.checkNotNull("Scope cannot be null", inScope);
    AssertValue.checkNotNull("Symbol cannot be null", symbol);

    final var searches = Map
        .of(SymbolCategory.FUNCTION, ErrorListener.SemanticClassification.DUPLICATE_NAME,
            SymbolCategory.TYPE, ErrorListener.SemanticClassification.DUPLICATE_TYPE,
            SymbolCategory.TEMPLATE_FUNCTION, ErrorListener.SemanticClassification.DUPLICATE_NAME,
            SymbolCategory.TEMPLATE_TYPE, ErrorListener.SemanticClassification.DUPLICATE_TYPE
        );

    //Note that we also stop same name as the types above.
    for (var entry : searches.entrySet()) {
      final var name = useQualifiedName ? symbol.getFullyQualifiedName() : symbol.getName();
      final var search = new SymbolSearch(name).setSearchType(entry.getKey());
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

    //Now it is possible the symbol will just resolve to itself. Which is obviously Ok as it is not a duplicate.
    final var symbolCheck = inScope.resolve(search);

    if (symbolCheck.isPresent()) {
      ISymbol possibleDuplicate = symbolCheck.get();
      if (possibleDuplicate != symbol) {
        final var message = String.format("'%s' as %s %s:",
            possibleDuplicate.getFriendlyName(), possibleDuplicate.getGenus(),
            locationExtractorFromSymbol.apply(possibleDuplicate));
        errorListener.semanticError(symbol.getSourceToken(), message, classificationError);
        return true;
      }
    }

    return false;
  }
}

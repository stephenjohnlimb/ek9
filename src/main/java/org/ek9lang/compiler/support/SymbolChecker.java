package org.ek9lang.compiler.support;

import java.util.Map;
import java.util.Optional;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.search.SymbolSearch;
import org.ek9lang.compiler.symbols.IScope;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.core.AssertValue;

/**
 * General utility class that searches in a scope for a symbol to check for duplicates.
 * Note that is a bit ore than a boolean check. It will formulate error messages and add
 * then to the error listener it has been provided with.
 */
public class SymbolChecker {

  private final ErrorListener errorListener;

  private final LocationExtractor locationExtractor = new LocationExtractor();

  public SymbolChecker(ErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  /**
   * Check for exising symbol in the scope.
   * if returns true then errors will have been added to the error listener.
   */
  public boolean errorsIfSymbolAlreadyDefined(IScope inScope, ISymbol symbol, boolean limitVarSearchToBlockScope) {
    AssertValue.checkNotNull("Scope cannot be null", inScope);
    AssertValue.checkNotNull("Symbol cannot be null", symbol);

    var searches = Map
        .of(ISymbol.SymbolCategory.FUNCTION, ErrorListener.SemanticClassification.DUPLICATE_FUNCTION,
            ISymbol.SymbolCategory.TYPE, ErrorListener.SemanticClassification.DUPLICATE_TYPE,
            ISymbol.SymbolCategory.TEMPLATE_FUNCTION, ErrorListener.SemanticClassification.DUPLICATE_FUNCTION,
            ISymbol.SymbolCategory.TEMPLATE_TYPE, ErrorListener.SemanticClassification.DUPLICATE_TYPE
        );

    for (var entry : searches.entrySet()) {
      var search = new SymbolSearch(symbol.getFullyQualifiedName()).setSearchType(entry.getKey());
      if (errorsIfResolved(inScope, symbol, search, entry.getValue())) {
        return true;
      }
    }
    //Need to do variables separate only use name not fully qualified name
    return errorsIfResolved(inScope, symbol,
        new SymbolSearch(symbol.getName()).setLimitToBlocks(limitVarSearchToBlockScope),
        ErrorListener.SemanticClassification.DUPLICATE_VARIABLE);
  }

  private boolean errorsIfResolved(IScope inScope, ISymbol symbol, SymbolSearch search,
                                   ErrorListener.SemanticClassification classificationError) {

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

package org.ek9lang.compiler.symbol.support;

import java.io.File;
import java.util.Map;
import java.util.Optional;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.symbol.IScope;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.support.search.SymbolSearch;
import org.ek9lang.core.exception.AssertValue;

/**
 * General utility class that searches in a scope for a symbol to check for duplicates.
 * Note that is a bit ore than a boolean check. It will formulate error messages and add
 * then to the error listener it has been provided with.
 */
public class SymbolChecker {

  private final ErrorListener errorListener;

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
      String message = String.format("'%s' on line %d already defined as %s in %s.",
          dup.getFriendlyName(), dup.getSourceToken().getLine(), dup.getGenus(),
          new File(dup.getSourceFileLocation()).getName());
      errorListener.semanticError(symbol.getSourceToken(), message, classificationError);
      return true;
    }
    return false;
  }
}

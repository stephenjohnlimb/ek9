package org.ek9lang.compiler.symbol.support;

import java.io.File;
import java.util.Optional;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.symbol.IScope;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.support.search.FunctionSymbolSearch;
import org.ek9lang.compiler.symbol.support.search.SymbolSearch;
import org.ek9lang.compiler.symbol.support.search.TypeSymbolSearch;
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
   * Check if there is a matching variable in the same scope.
   * Add errors to errorListener if there are.
   */
  public boolean errorsIfVariableSymbolAlreadyDefined(IScope inScope, ISymbol symbol)
  {
    return errorsIfSymbolAlreadyDefined(inScope, symbol, true);
  }

  /**
   * Check for exising symbol in the scope.
   * if returns true then errors will have been added to the error listener.
   */
  public boolean errorsIfSymbolAlreadyDefined(IScope inScope, ISymbol symbol, boolean limitVarSearchToBlockScope) {
    AssertValue.checkNotNull("Scope cannot be null", inScope);
    AssertValue.checkNotNull("Symbol cannot be null", symbol);

    //TODO need to check for duplicate definitions of generic types and generic functions?

    Optional<ISymbol> checkNotAlreadyDefined = inScope.resolve(new FunctionSymbolSearch(symbol.getName()));
    if (checkNotAlreadyDefined.isPresent()) {
      ISymbol dup = checkNotAlreadyDefined.get();
      String message = String.format("also found ' %s ' on line %d in %s.",
          dup.getFriendlyName(), dup.getSourceToken().getLine(), new File(dup.getSourceFileLocation()).getName());
      errorListener.semanticError(symbol.getSourceToken(), message,
          ErrorListener.SemanticClassification.DUPLICATE_FUNCTION);
      return true;
    }

    checkNotAlreadyDefined = inScope.resolve(new TypeSymbolSearch(symbol.getName()));
    if (checkNotAlreadyDefined.isPresent()) {
      ISymbol item = checkNotAlreadyDefined.get();
      String message = symbol.getName();
      if (item.getSourceToken() != null) {
        message += " line " + item.getSourceToken().getLine();
        message += " already defined with genus " + item.getGenus() + ".";
      }
      errorListener.semanticError(symbol.getSourceToken(), message,
          ErrorListener.SemanticClassification.DUPLICATE_TYPE);
      return true;
    }

    checkNotAlreadyDefined =
        inScope.resolve(new SymbolSearch(symbol.getName()).setLimitToBlocks(limitVarSearchToBlockScope));

    if (checkNotAlreadyDefined.isPresent()) {
      String message = "'" + symbol.getName() + "' line " + checkNotAlreadyDefined.get().getSourceToken().getLine();
      errorListener.semanticError(symbol.getSourceToken(), message,
          ErrorListener.SemanticClassification.DUPLICATE_VARIABLE);
      return true;
    }

    return false;
  }
}

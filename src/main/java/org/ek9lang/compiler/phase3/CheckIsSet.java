package org.ek9lang.compiler.phase3;

import java.util.function.Predicate;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.search.MethodSymbolSearchResult;
import org.ek9lang.compiler.support.LocationExtractor;
import org.ek9lang.compiler.symbols.FunctionSymbol;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.core.CompilerException;

/**
 * Given a symbol, this check that the type that the symbol has can support the '?' operation.
 * This includes variable/expressions that have a type that is a function.
 */
class CheckIsSet extends RuleSupport implements Predicate<ISymbol> {
  private final LocationExtractor locationExtractor = new LocationExtractor();
  private final SymbolTypeOrError symbolTypeOrError;
  private final CheckInitialised checkInitialised;

  CheckIsSet(final SymbolAndScopeManagement symbolAndScopeManagement,
             final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
    this.symbolTypeOrError = new SymbolTypeOrError(symbolAndScopeManagement, errorListener);
    this.checkInitialised = new CheckInitialised(symbolAndScopeManagement, errorListener);
  }

  @Override
  public boolean test(ISymbol symbol) {
    checkInitialised.accept(symbol);

    //Get the underlying type or emit error and return false.
    var symbolType = symbolTypeOrError.apply(symbol);

    if (symbolType.isEmpty()) {
      return false;
    }

    if (symbolType.get() instanceof FunctionSymbol) {
      return true;
    }

    if (symbolType.get() instanceof IAggregateSymbol aggregate) {
      var search = new MethodSymbolSearch("?")
          .setOfTypeOrReturn(symbolAndScopeManagement.getEk9Types().ek9Boolean());

      var results = aggregate.resolveMatchingMethods(search, new MethodSymbolSearchResult());
      if (results.isEmpty()) {
        var location = locationExtractor.apply(aggregate);
        var msg = "'" + search + "' wrt '"
            + aggregate.getName() + "' " + location + "':";

        errorListener.semanticError(symbol.getSourceToken(), msg,
            ErrorListener.SemanticClassification.OPERATOR_NOT_DEFINED);
      }
      return results.isSingleBestMatchPresent();
    }
    throw new CompilerException("Not expecting type to be " + symbolType);
  }
}

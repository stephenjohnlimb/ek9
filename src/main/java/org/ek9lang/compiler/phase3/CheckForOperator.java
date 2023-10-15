package org.ek9lang.compiler.phase3;

import java.util.Optional;
import java.util.function.Function;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.search.MethodSymbolSearchResult;
import org.ek9lang.compiler.support.LocationExtractor;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.core.CompilerException;

/**
 * To be used with operators on aggregates (except for the is-set ? operator, that has to deal with functions).
 * This function attempts to locate the method on the aggregate and returns the type of the return variable or empty.
 */
final class CheckForOperator extends RuleSupport implements Function<CheckOperatorData, Optional<ISymbol>> {
  private final LocationExtractor locationExtractor = new LocationExtractor();
  private final SymbolTypeOrError symbolTypeOrError;
  private final CheckInitialised checkInitialised;

  CheckForOperator(SymbolAndScopeManagement symbolAndScopeManagement,
                             ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
    this.symbolTypeOrError = new SymbolTypeOrError(symbolAndScopeManagement, errorListener);
    this.checkInitialised = new CheckInitialised(symbolAndScopeManagement, errorListener);
  }

  @Override
  public Optional<ISymbol> apply(CheckOperatorData checkOperatorData) {
    var symbol = checkOperatorData.symbol();
    checkInitialised.accept(symbol);

    //Get the underlying type or emit error and return false.
    //If the search is null it means that other errors would have been issued and no method lookup was possible.
    var symbolType = symbolTypeOrError.apply(symbol);
    if (symbolType.isEmpty() || checkOperatorData.search() == null) {
      return Optional.empty();
    }

    if (symbolType.get() instanceof IAggregateSymbol aggregate) {
      var search = checkOperatorData.search();
      var results = aggregate.resolveMatchingMethods(search, new MethodSymbolSearchResult());
      var bestMatch = results.getSingleBestMatchSymbol();
      if (bestMatch.isEmpty()) {
        var location = locationExtractor.apply(aggregate);
        var msg = "operator '" + search + "' is required on '"
            + symbol.getFriendlyName() + "', type defined " + location + ":";

        errorListener.semanticError(checkOperatorData.operatorUseToken(), msg,
            ErrorListener.SemanticClassification.OPERATOR_NOT_DEFINED);
        return Optional.empty();
      }
      return bestMatch.get().getReturningSymbol().getType();
    }
    throw new CompilerException("Not expecting type to be " + symbolType);
  }
}

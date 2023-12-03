package org.ek9lang.compiler.phase3;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.NO_MUTATION_IN_PURE_CONTEXT;
import static org.ek9lang.compiler.support.SymbolFactory.ACCESSED;

import java.util.Optional;
import java.util.function.Function;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.search.MethodSymbolSearchResult;
import org.ek9lang.compiler.support.LocationExtractorFromSymbol;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * To be used with operators on aggregates (except for the is-set ? operator, that has to deal with functions).
 * This function attempts to locate the method on the aggregate and returns the type of the return variable or empty.
 */
final class CheckForOperator extends TypedSymbolAccess implements Function<CheckOperatorData, Optional<ISymbol>> {
  private final LocationExtractorFromSymbol locationExtractorFromSymbol = new LocationExtractorFromSymbol();
  private final SymbolTypeOrEmpty symbolTypeOrEmpty = new SymbolTypeOrEmpty();

  CheckForOperator(final SymbolAndScopeManagement symbolAndScopeManagement,
                   final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);

  }

  @Override
  public Optional<ISymbol> apply(final CheckOperatorData checkOperatorData) {
    var symbol = checkOperatorData.symbol();

    Optional<ISymbol> rtn = Optional.empty();

    //Get the underlying type or emit error and return false.
    //If the search is null it means that other errors would have been issued and no method lookup was possible.
    var symbolType = symbolTypeOrEmpty.apply(symbol);
    if (symbolType.isPresent() && checkOperatorData.search() != null
        && symbolType.get() instanceof IAggregateSymbol aggregate) {
      var search = checkOperatorData.search();
      var results = aggregate.resolveMatchingMethods(search, new MethodSymbolSearchResult());
      var bestMatch = results.getSingleBestMatchSymbol();
      if (bestMatch.isPresent()) {
        var operator = bestMatch.get();
        noteOperatorAccessedIfConceptualType(aggregate, operator);
        //Now it depends where this operator is called and if it is pure or not.
        checkPureAccess(checkOperatorData.operatorUseToken(), operator);
        rtn = operator.getReturningSymbol().getType();
      } else {
        emitOperatorNotDefined(aggregate, checkOperatorData);
      }
    }
    return rtn;
  }

  private void emitOperatorNotDefined(final IAggregateSymbol aggregate, final CheckOperatorData checkOperatorData) {
    var location = locationExtractorFromSymbol.apply(aggregate);
    var msg = "operator '" + checkOperatorData.search() + "' is required on '"
        + checkOperatorData.symbol().getFriendlyName() + "', type first established " + location + ":";
    errorListener.semanticError(checkOperatorData.operatorUseToken(), msg,
        ErrorListener.SemanticClassification.OPERATOR_NOT_DEFINED);
  }

  private void checkPureAccess(final IToken operatorUseToken, final MethodSymbol operator) {

    if (!operator.isMarkedPure() && isProcessingScopePure()) {
      errorListener.semanticError(operatorUseToken, "'" + operator.getFriendlyName() + "':",
          NO_MUTATION_IN_PURE_CONTEXT);
    }
  }

  /**
   * We need to know if an operator has been used when employed in a 'T' in the generic type
   * This is needed later so that when a generic type is parameterised we can check each of the
   * types and see which operators have been accessed, then we can ensure the argument to the
   * parameterization has that operator.
   */
  private void noteOperatorAccessedIfConceptualType(final IAggregateSymbol aggregate, MethodSymbol operator) {
    if (aggregate.isConceptualTypeParameter()) {
      operator.putSquirrelledData(ACCESSED, "TRUE");
    }
  }
}

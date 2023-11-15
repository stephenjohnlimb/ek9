package org.ek9lang.compiler.phase3;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.NO_MUTATION_IN_PURE_CONTEXT;
import static org.ek9lang.compiler.support.SymbolFactory.ACCESSED;

import java.util.Optional;
import java.util.function.Function;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.search.MethodSymbolSearchResult;
import org.ek9lang.compiler.support.LocationExtractor;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.tokenizer.IToken;
import org.ek9lang.core.CompilerException;

/**
 * To be used with operators on aggregates (except for the is-set ? operator, that has to deal with functions).
 * This function attempts to locate the method on the aggregate and returns the type of the return variable or empty.
 */
final class CheckForOperator extends TypedSymbolAccess implements Function<CheckOperatorData, Optional<ISymbol>> {
  private final LocationExtractor locationExtractor = new LocationExtractor();
  private final SymbolTypeOrEmpty symbolTypeOrEmpty = new SymbolTypeOrEmpty();
  private final CheckInitialised checkInitialised;

  CheckForOperator(final SymbolAndScopeManagement symbolAndScopeManagement,
                   final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);

    this.checkInitialised = new CheckInitialised(symbolAndScopeManagement, errorListener);
  }

  @Override
  public Optional<ISymbol> apply(final CheckOperatorData checkOperatorData) {
    var symbol = checkOperatorData.symbol();
    checkInitialised.accept(symbol);

    //Get the underlying type or emit error and return false.
    //If the search is null it means that other errors would have been issued and no method lookup was possible.
    var symbolType = symbolTypeOrEmpty.apply(symbol);
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
            + symbol.getFriendlyName() + "', type first established " + location + ":";

        errorListener.semanticError(checkOperatorData.operatorUseToken(), msg,
            ErrorListener.SemanticClassification.OPERATOR_NOT_DEFINED);
        return Optional.empty();
      }
      var operator = bestMatch.get();
      noteOperatorAccessedIfConceptualType(aggregate, operator);
      //Now it depends where this operator is called and if it is pure or not.
      checkPureAccess(checkOperatorData.operatorUseToken(), operator);
      return operator.getReturningSymbol().getType();
    }
    throw new CompilerException("Not expecting type to be " + symbolType);
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

package org.ek9lang.compiler.phase3;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.NO_MUTATION_IN_PURE_CONTEXT;
import static org.ek9lang.compiler.support.SymbolFactory.ACCESSED;

import java.util.Optional;
import java.util.function.Function;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.common.TypedSymbolAccess;
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

    final var symbol = checkOperatorData.symbol();

    if (symbolIsActuallyAnEnumerationType(symbol)) {
      errorListener.semanticError(checkOperatorData.operatorUseToken(), "wrt '" + symbol.getName() + "':",
          ErrorListener.SemanticClassification.OPERATOR_CANNOT_BE_USED_ON_ENUMERATION);

      return Optional.empty();
    }

    //Get the underlying type or emit error and return false.
    //If the search is null it means that other errors would have been issued and no method lookup was possible.
    final var symbolType = symbolTypeOrEmpty.apply(symbol);

    if (symbolType.isPresent() && checkOperatorData.search() != null
        && symbolType.get() instanceof IAggregateSymbol aggregate) {

      final var search = checkOperatorData.search();
      final var results = aggregate.resolveMatchingMethods(search, new MethodSymbolSearchResult());
      final var bestMatch = results.getSingleBestMatchSymbol();

      if (bestMatch.isPresent()) {

        final var operator = bestMatch.get();
        noteOperatorAccessedIfConceptualType(aggregate, operator);
        //Now it depends where this operator is called and if it is pure or not.
        checkPureAccess(checkOperatorData.operatorUseToken(), operator);
        return operator.getReturningSymbol().getType();

      } else {
        emitOperatorNotDefined(aggregate, checkOperatorData);
      }
    }

    return Optional.empty();
  }

  /**
   * Now within EK9 we do need to reference the actual type of enumeration to be able to
   * iterate over its values. But we also need to stop silly use of operators on the actual
   * enumeration itself. So this is like needing 'iterator' to be a static method (like Java).
   * But other methods to be on the instance. That concept does not exist in EK9.
   */
  private boolean symbolIsActuallyAnEnumerationType(final ISymbol symbol) {
    return symbol.getGenus().equals(ISymbol.SymbolGenus.CLASS_ENUMERATION);
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

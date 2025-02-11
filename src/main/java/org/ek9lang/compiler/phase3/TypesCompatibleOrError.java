package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.support.LocationExtractorFromSymbol;
import org.ek9lang.compiler.support.SymbolMatcher;
import org.ek9lang.compiler.symbols.SymbolGenus;

/**
 * Check that the types of two symbol ar compatible with each other.
 * Or an error is emitted.
 * compatible means supers or traits are compatible or that the second var type can be
 * coerced to the first.
 * Note this also checks for abstract functions being on the right handside.
 */
final class TypesCompatibleOrError extends TypedSymbolAccess implements Consumer<TypeCompatibilityData> {
  private final SymbolMatcher matcher = new SymbolMatcher();
  private final LocationExtractorFromSymbol locationExtractorFromSymbol = new LocationExtractorFromSymbol();

  /**
   * Check symbols with types have compatible types.
   */
  TypesCompatibleOrError(final SymbolsAndScopes symbolsAndScopes,
                         final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);

  }

  @Override
  public void accept(final TypeCompatibilityData toCheck) {

    if (toCheck.lhs() != null
        && toCheck.rhs() != null
        && toCheck.lhs().getType().isPresent()
        && toCheck.rhs().getType().isPresent()) {
      rhsIsNotAbstractFunctionOrError(toCheck);
      typesCompatibleOrError(toCheck);
    }

  }

  private void rhsIsNotAbstractFunctionOrError(final TypeCompatibilityData toCheck) {

    //This should not be allowed, because you cannot call an abstract function.
    //So it is important to stop one being assigned anywhere.
    if (toCheck.rhs().getGenus().equals(SymbolGenus.FUNCTION_TRAIT)) {
      errorListener.semanticError(toCheck.location(), "'" + toCheck.rhs().getFriendlyName() + "':",
          ErrorListener.SemanticClassification.BAD_ABSTRACT_FUNCTION_USE);
    }

  }

  private void typesCompatibleOrError(final TypeCompatibilityData toCheck) {

    final var fromType = toCheck.rhs().getType();
    final var toType = toCheck.lhs().getType();
    final var position = locationExtractorFromSymbol.apply(toCheck.lhs());
    final var weightOfMatch = matcher.getWeightOfMatch(fromType, toType);

    if (weightOfMatch < 0.0) {
      final var msg = "'" + toCheck.lhs().getFriendlyName() + "' "
          + position + " and '"
          + toCheck.rhs().getFriendlyName() + "':";

      errorListener.semanticError(toCheck.location(), msg,
          ErrorListener.SemanticClassification.INCOMPATIBLE_TYPES);
    }

  }
}

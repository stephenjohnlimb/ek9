package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.support.LocationExtractor;
import org.ek9lang.compiler.support.SymbolMatcher;

/**
 * Check that the types of two symbol ar compatible with each other.
 * Or an error is emitted.
 * compatible means supers or traits are compatible or that the second var type can be
 * coerced to the first.
 */
final class CheckTypesCompatible extends TypedSymbolAccess implements Consumer<
    TypeCompatibilityData> {
  private final SymbolMatcher matcher = new SymbolMatcher();
  private final LocationExtractor locationExtractor = new LocationExtractor();

  /**
   * Check symbols with types have compatible types.
   */
  CheckTypesCompatible(final SymbolAndScopeManagement symbolAndScopeManagement,
                       final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);

  }

  @Override
  public void accept(final TypeCompatibilityData toCheck) {
    if (toCheck.lhs() != null
        && toCheck.rhs() != null
        && toCheck.lhs().getType().isPresent()
        && toCheck.rhs().getType().isPresent()) {

      var fromType = toCheck.rhs().getType();
      var toType = toCheck.lhs().getType();

      var position = locationExtractor.apply(toCheck.lhs());

      var weightOfMatch = matcher.getWeightOfMatch(fromType, toType);
      if (weightOfMatch < 0.0) {
        var msg = "'" + toCheck.lhs().getFriendlyName() + "' "
            + position + " and '"
            + toCheck.rhs().getFriendlyName() + "':";
        errorListener.semanticError(toCheck.location(), msg,
            ErrorListener.SemanticClassification.INCOMPATIBLE_TYPES);
      }
    }
  }
}

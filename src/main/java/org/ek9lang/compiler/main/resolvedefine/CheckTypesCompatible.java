package org.ek9lang.compiler.main.resolvedefine;

import java.util.function.Consumer;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.main.phases.definition.SymbolAndScopeManagement;
import org.ek9lang.compiler.support.RuleSupport;
import org.ek9lang.compiler.symbol.support.LocationExtractor;
import org.ek9lang.compiler.symbol.support.SymbolMatcher;

/**
 * Check that the types of two symbol ar compatible with each other.
 * Or an error is emitted.
 * compatible means supers or traits are compatible or that the second var type can be
 * coerced to the first.
 */
public class CheckTypesCompatible extends RuleSupport implements Consumer<TypeCompatibilityData> {

  private final SymbolMatcher matcher = new SymbolMatcher();

  private final LocationExtractor locationExtractor = new LocationExtractor();

  /**
   * Check symbols with types have compatible types.
   */
  public CheckTypesCompatible(final SymbolAndScopeManagement symbolAndScopeManagement,
                              final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);

  }

  @Override
  public void accept(TypeCompatibilityData toCheck) {
    if (toCheck.lhs() != null
        && toCheck.rhs() != null
        && toCheck.lhs().getType().isPresent()
        && toCheck.rhs().getType().isPresent()) {

      var fromType = toCheck.rhs().getType();
      var toType = toCheck.lhs().getType();

      var position = locationExtractor.apply(toCheck.lhs());

      var weightOfMatch = matcher.getWeightOfMatch(fromType, toType);
      if (weightOfMatch < 0.0) {
        var msg = "'" + toCheck.rhs().getFriendlyName() + "' and '"
            + toCheck.lhs().getFriendlyName() + "' "
            + position + ":";
        errorListener.semanticError(toCheck.location(), msg,
            ErrorListener.SemanticClassification.INCOMPATIBLE_TYPES);
      }
    }
  }
}

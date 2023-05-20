package org.ek9lang.compiler.main.resolvedefine;

import java.util.function.Consumer;
import org.ek9lang.compiler.errors.ErrorListener;

/**
 * Check that the types of two symbol ar compatible with each other.
 * Or an error is emitted.
 * compatible means supers or traits are compatible or that the second var type can be
 * coerced to the first.
 */
public class CheckTypesCompatible implements Consumer<TypeCompatibilityData> {
  private final ErrorListener errorListener;

  /**
   * Check symbols with types have compatible types.
   */
  public CheckTypesCompatible(final ErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  @Override
  public void accept(TypeCompatibilityData toCheck) {
    if (toCheck.lhs() != null && toCheck.rhs() != null
        && toCheck.lhs().getType().isPresent() && toCheck.rhs().getType().isPresent()
        && !toCheck.rhs().getType().get().isAssignableTo(toCheck.lhs().getType())) {
      var msg = "'" + toCheck.lhs().getFriendlyName() + "' and '" + toCheck.rhs().getFriendlyName() + "':";
      errorListener.semanticError(toCheck.location(), msg,
          ErrorListener.SemanticClassification.INCOMPATIBLE_TYPES);
    } //Else another un typed error will have been issued.
  }
}

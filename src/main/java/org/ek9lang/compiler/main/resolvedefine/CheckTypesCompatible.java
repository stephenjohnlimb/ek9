package org.ek9lang.compiler.main.resolvedefine;

import java.util.function.BiConsumer;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.symbol.ISymbol;

/**
 * Check that the types of two symbol ar compatible with each other.
 * Or an error is emitted.
 * compatible means supers or traits are compatible or that the second var type can be
 * coerced to the first.
 */
public class CheckTypesCompatible implements BiConsumer<ISymbol, ISymbol> {
  private final ErrorListener errorListener;

  /**
   * Check symbols with types have compatible types.
   */
  public CheckTypesCompatible(final ErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  @Override
  public void accept(ISymbol o1, ISymbol o2) {
    if (o1.getType().isPresent() && o2.getType().isPresent()
        && !o2.getType().get().isAssignableTo(o1.getType())) {
      var msg = "'" + o1.getFriendlyName() + "' and '" + o2.getFriendlyName() + "':";
      errorListener.semanticError(o1.getSourceToken(), msg,
          ErrorListener.SemanticClassification.INCOMPATIBLE_TYPES);
    } //Else another un typed error will have been issued.
  }
}

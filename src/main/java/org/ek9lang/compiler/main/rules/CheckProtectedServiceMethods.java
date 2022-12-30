package org.ek9lang.compiler.main.rules;

import java.util.function.Consumer;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.symbol.AggregateSymbol;
import org.ek9lang.compiler.symbol.ServiceOperationSymbol;

/**
 * Ensure that any non web service operation methods (i.e. general methods) are marked as private/public.
 */
public class CheckProtectedServiceMethods implements Consumer<AggregateSymbol> {

  private final ErrorListener errorListener;

  public CheckProtectedServiceMethods(final ErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  @Override
  public void accept(AggregateSymbol aggregateSymbol) {
    aggregateSymbol.getAllNonAbstractMethods().forEach(method -> {
      if (!(method instanceof ServiceOperationSymbol)) {
        //Then it is just a normal method
        if ("protected".equals(method.getAccessModifier())) {
          errorListener.semanticError(method.getSourceToken(),
              "'" + method.getName() + "' in service '" + aggregateSymbol.getFriendlyName() + "'",
              ErrorListener.SemanticClassification.METHOD_MODIFIER_PROTECTED_IN_SERVICE);
        }
      }
    });

  }
}

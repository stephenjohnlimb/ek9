package org.ek9lang.compiler.main.rules;

import java.util.List;
import java.util.function.Consumer;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.symbol.ServiceOperationSymbol;

/**
 * Error when the definition of a service operator is invalid.
 */
public class CheckForInvalidServiceOperator implements Consumer<ServiceOperationSymbol> {
  private final ErrorListener errorListener;

  private static final List<String> supportedOperators = List.of(
      "+", "+=", "-", "-=", ":^:", ":~:", "?");

  public CheckForInvalidServiceOperator(final ErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  @Override
  public void accept(final ServiceOperationSymbol serviceSymbol) {
    var operator = serviceSymbol.getName();
    if (!supportedOperators.contains(operator)) {
      errorListener.semanticError(serviceSymbol.getSourceToken(), "using '" + operator + "' as",
          ErrorListener.SemanticClassification.SERVICE_OPERATOR_NOT_SUPPORTED);
    }
  }
}

package org.ek9lang.compiler.support;

import java.util.List;
import java.util.function.Consumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.symbols.ServiceOperationSymbol;

/**
 * Error when the definition of a service operator is invalid.
 */
public class CheckForInvalidServiceOperator implements Consumer<ServiceOperationSymbol> {
  private static final List<String> supportedOperators = List.of(
      "+", "+=", "-", "-=", ":^:", ":~:", "?");
  private final ErrorListener errorListener;

  CheckForInvalidServiceOperator(final ErrorListener errorListener) {

    this.errorListener = errorListener;

  }

  @Override
  public void accept(final ServiceOperationSymbol serviceSymbol) {

    final var operator = serviceSymbol.getName();

    if (!supportedOperators.contains(operator)) {
      errorListener.semanticError(serviceSymbol.getSourceToken(), "using '" + operator + "' as",
          ErrorListener.SemanticClassification.SERVICE_OPERATOR_NOT_SUPPORTED);
    }

  }
}

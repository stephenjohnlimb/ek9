package org.ek9lang.compiler.support;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.SERVICE_URI_WITH_VARS_NOT_SUPPORTED;

import java.util.function.Consumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.symbols.AggregateSymbol;

/**
 * Error when the definition of a service is invalid.
 */
public class CheckForInvalidServiceDefinition implements Consumer<AggregateSymbol> {
  private final ErrorListener errorListener;

  CheckForInvalidServiceDefinition(final ErrorListener errorListener) {

    this.errorListener = errorListener;

  }

  @Override
  public void accept(final AggregateSymbol serviceSymbol) {

    final var uri = serviceSymbol.getSquirrelledData("HTTPURI");

    if (uri.contains("{") || uri.contains("}")) {
      final var msg = "using '" + uri + "' as";
      errorListener.semanticError(serviceSymbol.getSourceToken(), msg, SERVICE_URI_WITH_VARS_NOT_SUPPORTED);
    }

  }
}

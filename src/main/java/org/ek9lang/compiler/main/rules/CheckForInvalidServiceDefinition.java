package org.ek9lang.compiler.main.rules;

import java.util.function.Consumer;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.symbols.AggregateSymbol;

/**
 * Error when the definition of a service is invalid.
 */
public class CheckForInvalidServiceDefinition implements Consumer<AggregateSymbol> {
  private final ErrorListener errorListener;

  public CheckForInvalidServiceDefinition(final ErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  @Override
  public void accept(final AggregateSymbol serviceSymbol) {
    var uri = serviceSymbol.getSquirrelledData("HTTPURI");
    if (uri.contains("{") || uri.contains("}")) {
      errorListener.semanticError(serviceSymbol.getSourceToken(), "using '" + uri + "' as",
          ErrorListener.SemanticClassification.SERVICE_URI_WITH_VARS_NOT_SUPPORTED);
    }
  }
}

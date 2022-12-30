package org.ek9lang.compiler.errors;

import java.util.function.Consumer;
import org.antlr.v4.runtime.Token;
import org.ek9lang.compiler.symbol.AggregateSymbol;

/**
 * Error when the definition of a service in invalid.
 */
public class InvalidServiceDefinition implements Consumer<AggregateSymbol> {
  private final ErrorListener errorListener;

  public InvalidServiceDefinition(final ErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  @Override
  public void accept(final AggregateSymbol serviceSymbol) {
    var uri = serviceSymbol.getSquirrelledData("HTTPURI");
    errorListener.semanticError(serviceSymbol.getSourceToken(), "using '" + uri + "' as",
        ErrorListener.SemanticClassification.SERVICE_URI_WITH_VARS_NOT_SUPPORTED);
  }
}

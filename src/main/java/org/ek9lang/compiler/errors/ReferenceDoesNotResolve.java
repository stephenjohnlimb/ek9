package org.ek9lang.compiler.errors;

import java.util.function.BiConsumer;
import org.antlr.v4.runtime.Token;

/**
 * Error when a reference is made, but it cannot be resolved.
 */
public class ReferenceDoesNotResolve implements BiConsumer<Token, String> {
  private final ErrorListener errorListener;

  public ReferenceDoesNotResolve(final ErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  @Override
  public void accept(Token token, String fullyQualifiedIdentifier) {
    errorListener.semanticError(token, "'" + fullyQualifiedIdentifier + "'",
        ErrorListener.SemanticClassification.REFERENCE_DOES_NOT_RESOLVED);
  }
}

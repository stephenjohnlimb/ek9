package org.ek9lang.compiler.phase1;

import java.util.function.BiConsumer;
import org.antlr.v4.runtime.Token;
import org.ek9lang.compiler.common.ErrorListener;

/**
 * Error when a reference is made, but it cannot be resolved.
 */
final class ReferenceDoesNotResolve implements BiConsumer<Token, String> {
  private final ErrorListener errorListener;

  ReferenceDoesNotResolve(final ErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  @Override
  public void accept(Token token, String fullyQualifiedIdentifier) {
    errorListener.semanticError(token, "'" + fullyQualifiedIdentifier + "'",
        ErrorListener.SemanticClassification.REFERENCE_DOES_NOT_RESOLVED);
  }
}

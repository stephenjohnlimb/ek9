package org.ek9lang.compiler.phase1;

import java.util.function.BiConsumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * Error when a reference is made, but it cannot be resolved.
 */
final class ReferenceDoesNotResolve implements BiConsumer<IToken, String> {
  private final ErrorListener errorListener;

  ReferenceDoesNotResolve(final ErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  @Override
  public void accept(final IToken token, final String fullyQualifiedIdentifier) {
    errorListener.semanticError(token, "'" + fullyQualifiedIdentifier + "'",
        ErrorListener.SemanticClassification.REFERENCE_DOES_NOT_RESOLVED);
  }
}

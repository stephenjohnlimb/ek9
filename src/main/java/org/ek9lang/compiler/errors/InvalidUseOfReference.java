package org.ek9lang.compiler.errors;

import java.util.function.Consumer;
import org.antlr.v4.runtime.Token;

/**
 * Error when a reference is attempted by the syntax is incorrect.
 */
public class InvalidUseOfReference implements Consumer<Token> {
  private final ErrorListener errorListener;

  public InvalidUseOfReference(final ErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  @Override
  public void accept(final Token token) {
    errorListener.semanticError(token, "must have '::' qualifier,",
        ErrorListener.SemanticClassification.INVALID_SYMBOL_BY_REFERENCE);
  }
}

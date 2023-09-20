package org.ek9lang.compiler.support;

import java.util.function.BiConsumer;
import org.antlr.v4.runtime.Token;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * Error when the definition of an enumerated values could be invalid.
 */
final class InvalidEnumeratedValue implements BiConsumer<IToken, Token> {
  private final ErrorListener errorListener;

  InvalidEnumeratedValue(final ErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  @Override
  public void accept(final IToken newEnumeratedValueToken, final Token existingEnumeratedValueToken) {
    var msg = String.format("and '%s' on line %d",
        existingEnumeratedValueToken.getText(),
        existingEnumeratedValueToken.getLine());
    errorListener.semanticError(newEnumeratedValueToken, msg,
        ErrorListener.SemanticClassification.POSSIBLE_DUPLICATE_ENUMERATED_VALUE);
  }
}

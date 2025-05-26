package org.ek9lang.compiler.phase1;

import java.util.function.BiConsumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * To be called when an unreachable statement is encountered to emit an error.
 * The first token is the point that is unreachable, the second token is
 * the cause of why this point is unreachable.
 */
final class EmitUnreachableStatementError implements BiConsumer<IToken, IToken> {

  private final ErrorListener errorListener;

  EmitUnreachableStatementError(final ErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  @Override
  public void accept(final IToken unreachablePoint, final IToken reasonExceptionPoint) {

    final var message = String.format("Unreachable, because of '%s' on line %d:",
        reasonExceptionPoint.getText(),
        reasonExceptionPoint.getLine());

    errorListener.semanticError(unreachablePoint, message,
        ErrorListener.SemanticClassification.STATEMENT_UNREACHABLE);
  }

}

package org.ek9lang.compiler.phase1;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.errors.ErrorListener;

/**
 * Error when a reference is attempted by the syntax is incorrect.
 */
final class CheckForInvalidUseOfReference implements Consumer<EK9Parser.IdentifierReferenceContext> {
  private final ErrorListener errorListener;

  CheckForInvalidUseOfReference(final ErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  @Override
  public void accept(final EK9Parser.IdentifierReferenceContext ctx) {
    if (!ctx.getText().contains("::")) {
      errorListener.semanticError(ctx.start, "must have '::' qualifier,",
          ErrorListener.SemanticClassification.INVALID_SYMBOL_BY_REFERENCE);
    }
  }
}

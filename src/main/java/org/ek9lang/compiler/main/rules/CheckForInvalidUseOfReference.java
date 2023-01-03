package org.ek9lang.compiler.main.rules;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.errors.ErrorListener;

/**
 * Error when a reference is attempted by the syntax is incorrect.
 */
public class CheckForInvalidUseOfReference implements Consumer<EK9Parser.IdentifierReferenceContext> {
  private final ErrorListener errorListener;

  public CheckForInvalidUseOfReference(final ErrorListener errorListener) {
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

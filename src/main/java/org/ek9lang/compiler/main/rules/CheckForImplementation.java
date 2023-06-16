package org.ek9lang.compiler.main.rules;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.errors.ErrorListener;

/**
 * Typically used with programs. cannot be abstract and so must always provide some form of implementation.
 */
public class CheckForImplementation implements Consumer<EK9Parser.MethodDeclarationContext> {

  private final CheckForBody checkForBody = new CheckForBody();
  private final ErrorListener errorListener;

  public CheckForImplementation(final ErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  @Override
  public void accept(EK9Parser.MethodDeclarationContext ctx) {
    final var hasBody = checkForBody.test(ctx);
    final var isVirtual = ctx.ABSTRACT() == null && !hasBody;

    if (ctx.operationDetails() == null || isVirtual) {
      errorListener.semanticError(ctx.start, "", ErrorListener.SemanticClassification.IMPLEMENTATION_MUST_BE_PROVIDED);
    }
  }
}

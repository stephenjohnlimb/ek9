package org.ek9lang.compiler.phase1;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.ProcessingBodyPresent;

/**
 * Typically used with programs. cannot be abstract and so must always provide some form of implementation.
 */
final class ImplementationPresentOrError implements Consumer<EK9Parser.MethodDeclarationContext> {
  private final ProcessingBodyPresent processingBodyPresent = new ProcessingBodyPresent();
  private final ErrorListener errorListener;

  ImplementationPresentOrError(final ErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  @Override
  public void accept(EK9Parser.MethodDeclarationContext ctx) {

    final var hasBody = processingBodyPresent.test(ctx.operationDetails());
    final var isVirtual = ctx.ABSTRACT() == null && !hasBody;

    if (ctx.operationDetails() == null || isVirtual) {
      errorListener.semanticError(ctx.start, "", ErrorListener.SemanticClassification.IMPLEMENTATION_MUST_BE_PROVIDED);
    }

  }
}

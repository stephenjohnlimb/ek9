package org.ek9lang.compiler.phase1;

import java.util.function.BiConsumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.ExternallyImplemented;
import org.ek9lang.compiler.common.ProcessingBodyPresent;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Typically used with programs. cannot be abstract and so must always provide some form of implementation.
 * Unless it is in an external module - then it just exists there - and could actually be called.
 */
final class ImplementationPresentOrError implements BiConsumer<ISymbol, EK9Parser.MethodDeclarationContext> {
  private final ProcessingBodyPresent processingBodyPresent = new ProcessingBodyPresent();
  private final ExternallyImplemented externallyImplemented = new ExternallyImplemented();
  private final ErrorListener errorListener;

  ImplementationPresentOrError(final ErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  @Override
  public void accept(final ISymbol aggregate, EK9Parser.MethodDeclarationContext ctx) {
    //Nothing to check is externally implemented.
    if (externallyImplemented.test(aggregate)) {
      return;
    }

    final var hasBody = processingBodyPresent.test(ctx.operationDetails());
    final var isVirtual = ctx.ABSTRACT() == null && !hasBody;

    if (ctx.operationDetails() == null || isVirtual) {
      errorListener.semanticError(ctx.start, "", ErrorListener.SemanticClassification.IMPLEMENTATION_MUST_BE_PROVIDED);
    }

  }
}

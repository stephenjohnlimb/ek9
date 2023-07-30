package org.ek9lang.compiler.phase1;

import java.util.function.BiConsumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.symbols.MethodSymbol;

/**
 * Check not dispatcher methods.
 */
final class CheckNotDispatcherMethod implements BiConsumer<MethodSymbol, EK9Parser.MethodDeclarationContext> {

  private final ErrorListener errorListener;

  CheckNotDispatcherMethod(final ErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  @Override
  public void accept(final MethodSymbol method, final EK9Parser.MethodDeclarationContext ctx) {

    final var message = "for method '" + method.getName() + "':";
    if (method.isMarkedAsDispatcher()) {
      errorListener.semanticError(method.getSourceToken(), message,
          ErrorListener.SemanticClassification.DISPATCH_ONLY_SUPPORTED_IN_CLASSES);
    }
  }
}

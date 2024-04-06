package org.ek9lang.compiler.common;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.TRAITS_DO_NOT_HAVE_CONSTRUCTORS;

import java.util.function.BiConsumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.symbols.MethodSymbol;

/**
 * Check trait specifics on methods/operators, Allowing missing body but marking as abstract.
 */
public class CheckTraitMethod implements BiConsumer<MethodSymbol, EK9Parser.OperationDetailsContext> {
  private final CheckForBody checkForBody = new CheckForBody();
  private final ErrorListener errorListener;

  /**
   * Create new checker.
   */
  public CheckTraitMethod(final ErrorListener errorListener) {

    this.errorListener = errorListener;

  }

  @Override
  public void accept(final MethodSymbol method, final EK9Parser.OperationDetailsContext ctx) {

    final var hasBody = checkForBody.test(ctx);
    final var isVirtual = !method.isMarkedAbstract() && !hasBody;

    if (method.isConstructor()) {
      errorListener.semanticError(method.getSourceToken(), "", TRAITS_DO_NOT_HAVE_CONSTRUCTORS);
    } else if (isVirtual) {
      method.setMarkedAbstract(true);
    }
  }
}

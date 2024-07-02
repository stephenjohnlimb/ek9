package org.ek9lang.compiler.common;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.TRAITS_DO_NOT_HAVE_CONSTRUCTORS;

import java.util.function.BiConsumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.symbols.MethodSymbol;

/**
 * Check trait specifics on constructors/methods/operators, Allowing missing body but marking as abstract.
 * Issues error if method is marked as a constructor on a trait.
 */
public class ProcessTraitMethodOrError implements BiConsumer<MethodSymbol, EK9Parser.OperationDetailsContext> {
  private final ProcessingBodyPresent processingBodyPresent = new ProcessingBodyPresent();
  private final ErrorListener errorListener;

  /**
   * Create new checker.
   */
  public ProcessTraitMethodOrError(final ErrorListener errorListener) {

    this.errorListener = errorListener;

  }

  @Override
  public void accept(final MethodSymbol method, final EK9Parser.OperationDetailsContext ctx) {
    if (!method.isConstructor()) {
      markMethodAbstractIfVirtual(method, ctx);
    } else {
      errorListener.semanticError(method.getSourceToken(), "", TRAITS_DO_NOT_HAVE_CONSTRUCTORS);
    }
  }

  private void markMethodAbstractIfVirtual(final MethodSymbol method, final EK9Parser.OperationDetailsContext ctx) {

    if (!method.isMarkedAbstract() && !processingBodyPresent.test(ctx)) {
      method.setMarkedAbstract(true);
    }
  }

}

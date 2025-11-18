package org.ek9lang.compiler.common;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.DISPATCHER_BUT_NO_BODY_PROVIDED;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.NOT_ABSTRACT_AND_NO_BODY_PROVIDED;

import java.util.function.BiConsumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.symbols.MethodSymbol;

/**
 * Check trait specifics on methods/operators.
 */
public class TraitMethodAcceptableOrError implements BiConsumer<MethodSymbol, EK9Parser.OperationDetailsContext> {
  private final ProcessingBodyPresent processingBodyPresent = new ProcessingBodyPresent();
  private final ExternallyImplemented externallyImplemented = new ExternallyImplemented();
  private final Defaulted defaulted = new Defaulted();
  private final ErrorListener errorListener;

  /**
   * Create new.
   */
  public TraitMethodAcceptableOrError(final ErrorListener errorListener) {

    this.errorListener = errorListener;

  }

  @Override
  public void accept(final MethodSymbol method, final EK9Parser.OperationDetailsContext ctx) {
    //So for general methods if they are not marked as abstract and have not supplied body
    //Then we must check if it is marked as default or is externally provided.

    final var isVirtual = !method.isMarkedAbstract() && !processingBodyPresent.test(ctx);
    final var isDefaultedMethod = defaulted.test(method);
    final var isExternallyImplemented = externallyImplemented.test(method);

    if (isVirtual && !isDefaultedMethod && !isExternallyImplemented) {
      if (method.isMarkedAsDispatcher() && !method.isOperator()) {
        errorListener.semanticError(method.getSourceToken(), "", DISPATCHER_BUT_NO_BODY_PROVIDED);
      } else {
        errorListener.semanticError(method.getSourceToken(), "", NOT_ABSTRACT_AND_NO_BODY_PROVIDED);
      }
    }

    // Note: DEFAULT_AND_ABSTRACT check removed - unreachable condition
    // (default operator auto-generates implementations, cannot be marked as abstract in source code)
  }

}
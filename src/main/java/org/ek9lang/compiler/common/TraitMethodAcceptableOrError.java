package org.ek9lang.compiler.common;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.DEFAULT_AND_ABSTRACT;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.NOT_ABSTRACT_AND_NO_BODY_PROVIDED;
import static org.ek9lang.compiler.support.SymbolFactory.DEFAULTED;

import java.util.function.BiConsumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.symbols.MethodSymbol;

/**
 * Check trait specifics on methods/operators.
 */
public class TraitMethodAcceptableOrError implements BiConsumer<MethodSymbol, EK9Parser.OperationDetailsContext> {
  private final ProcessingBodyPresent processingBodyPresent = new ProcessingBodyPresent();
  private final ExternallyImplemented externallyImplemented = new ExternallyImplemented();
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

    final var hasBody = processingBodyPresent.test(ctx);
    final var isVirtual = !method.isMarkedAbstract() && !hasBody;
    final var isDefaultedMethod = "TRUE".equals(method.getSquirrelledData(DEFAULTED));
    final var isExternallyImplemented = externallyImplemented.test(method);

    if (isVirtual && !isDefaultedMethod && !isExternallyImplemented) {
      errorListener.semanticError(method.getSourceToken(), "", NOT_ABSTRACT_AND_NO_BODY_PROVIDED);
    }

    if (isDefaultedMethod && method.isMarkedAbstract()) {
      errorListener.semanticError(method.getSourceToken(), "", DEFAULT_AND_ABSTRACT);
    }
  }

}
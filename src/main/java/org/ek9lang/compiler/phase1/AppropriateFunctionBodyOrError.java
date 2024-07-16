package org.ek9lang.compiler.phase1;

import java.util.function.BiConsumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.ExternallyImplemented;
import org.ek9lang.compiler.common.ProcessingBodyPresent;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.symbols.FunctionSymbol;

/**
 * Checks if the body of a function is appropriate.
 * i.e has it correctly been marked as abstract.
 */
final class AppropriateFunctionBodyOrError extends RuleSupport implements
    BiConsumer<FunctionSymbol, EK9Parser.OperationDetailsContext> {

  private final ProcessingBodyPresent processingBodyPresent = new ProcessingBodyPresent();
  private final ExternallyImplemented externallyImplemented = new ExternallyImplemented();

  AppropriateFunctionBodyOrError(final SymbolsAndScopes symbolsAndScopes,
                                 final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);

  }

  @Override
  public void accept(final FunctionSymbol functionSymbol,
                     final EK9Parser.OperationDetailsContext ctx) {

    final var hasBody = processingBodyPresent.test(ctx);
    final var isAbstract = functionSymbol.isMarkedAbstract();
    final var isExternallyImplemented = externallyImplemented.test(functionSymbol);

    if (hasBody && isAbstract) {
      errorListener.semanticError(functionSymbol.getSourceToken(), "",
          ErrorListener.SemanticClassification.ABSTRACT_BUT_BODY_PROVIDED);
    } else if (!hasBody && !(isAbstract || isExternallyImplemented)) {
      errorListener.semanticError(functionSymbol.getSourceToken(), "",
          ErrorListener.SemanticClassification.NOT_ABSTRACT_AND_NO_BODY_PROVIDED);
    }

  }
}

package org.ek9lang.compiler.symbols.support;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;

/**
 * Checks that the use of the variable only if it has a web variable correlation is valid.
 * i.e. only when used in a service.
 * This works on the structure of the grammar, so it a bit fragile if grammar is refactored.
 */
public class CheckAppropriateWebVariable implements Consumer<EK9Parser.VariableOnlyDeclarationContext> {
  private final ErrorListener errorListener;

  CheckAppropriateWebVariable(final ErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  @Override
  public void accept(EK9Parser.VariableOnlyDeclarationContext ctx) {
    //only check if a web variable correlation has been specified
    if (ctx.webVariableCorrelation() == null) {
      return;
    }

    if (ctx.getParent() instanceof EK9Parser.ArgumentParamContext argumentParam
        && argumentParam.getParent() instanceof EK9Parser.OperationDetailsContext operationDetails
        && operationDetails.parent instanceof EK9Parser.ServiceOperationDeclarationContext) {
      return;
    }

    errorListener.semanticError(ctx.webVariableCorrelation().start, "Web Service",
        ErrorListener.SemanticClassification.SERVICE_HTTP_ACCESS_NOT_SUPPORTED);
  }
}

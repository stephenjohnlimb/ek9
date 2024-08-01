package org.ek9lang.compiler.phase3;

import java.util.List;
import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.support.SymbolFactory;
import org.ek9lang.compiler.symbols.CaptureScope;
import org.ek9lang.compiler.tokenizer.Ek9Token;

/**
 * Resolves the variables to be captured and defines new variable symbols against the appropriate scopes.
 */
final class DynamicCaptureOrError extends TypedSymbolAccess
    implements Consumer<EK9Parser.DynamicVariableCaptureContext> {
  private final SymbolFactory symbolFactory;

  /**
   * Define new variables in the capture scope as they are resolved.
   */
  DynamicCaptureOrError(final SymbolsAndScopes symbolsAndScopes,
                        final ErrorListener errorListener,
                        final SymbolFactory symbolFactory) {

    super(symbolsAndScopes, errorListener);
    this.symbolFactory = symbolFactory;

  }

  @Override
  public void accept(final EK9Parser.DynamicVariableCaptureContext ctx) {

    if (symbolsAndScopes.getRecordedScope(ctx) instanceof CaptureScope captureScope) {

      //So now all the 'stuff' we should have resolved should be attached to the appropriate ctx's
      getParamContexts(ctx).forEach(param -> {

        //Default to name in expression might just be a variable
        //Earlier phases will have checked validity, but if identifier is used then use that instead
        var variableName = param.expression().getText();
        if (param.identifier() != null) {
          variableName = param.identifier().getText();
        }
        final var resolvedCapture = getRecordedAndTypedSymbol(param.expression());
        if (resolvedCapture != null) {
          //Might have had resolution issues earlier so might not be set.
          final var newCapturedSymbol =
              symbolFactory.newVariable(variableName, new Ek9Token(param.start), false, false);
          newCapturedSymbol.setType(resolvedCapture.getType());
          newCapturedSymbol.setInitialisedBy(resolvedCapture.getInitialisedBy());
          captureScope.define(newCapturedSymbol);
        }

      });
      captureScope.setOpenToEnclosingScope(false);
    }
  }

  private List<EK9Parser.ExpressionParamContext> getParamContexts(final EK9Parser.DynamicVariableCaptureContext ctx) {
    if (ctx.paramExpression() == null || ctx.paramExpression().isEmpty()) {
      return List.of();
    }

    return ctx.paramExpression().expressionParam();
  }
}

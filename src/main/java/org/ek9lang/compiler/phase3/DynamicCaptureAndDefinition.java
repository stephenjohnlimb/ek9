package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.symbols.CaptureScope;
import org.ek9lang.compiler.symbols.support.SymbolFactory;

/**
 * Resolves the variables to be captured and defines new variable symbols against the appropriate scopes.
 */
final class DynamicCaptureAndDefinition extends RuleSupport
    implements Consumer<EK9Parser.DynamicVariableCaptureContext> {
  private final SymbolFactory symbolFactory;

  /**
   * Define new variables in the capture scope as they are resolved.
   */
  DynamicCaptureAndDefinition(final SymbolAndScopeManagement symbolAndScopeManagement,
                              final ErrorListener errorListener,
                              final SymbolFactory symbolFactory) {
    super(symbolAndScopeManagement, errorListener);
    this.symbolFactory = symbolFactory;
  }

  @Override
  public void accept(EK9Parser.DynamicVariableCaptureContext ctx) {
    CaptureScope scope = (CaptureScope) symbolAndScopeManagement.getRecordedScope(ctx);
    //So now all the 'stuff' we should have resolved should be attached to the appropriate ctx's
    if (ctx.paramExpression() != null
        && !ctx.paramExpression().isEmpty()
        && !ctx.paramExpression().expressionParam().isEmpty()) {
      ctx.paramExpression().expressionParam().forEach(param -> {
        //Default to name in expression might just be a variable
        //Earlier phases will have checked validity, but if identifier is used then use that instead
        var variableName = param.expression().getText();
        if (param.identifier() != null) {
          variableName = param.identifier().getText();
        }
        var resolvedCapture = symbolAndScopeManagement.getRecordedSymbol(param.expression());
        if (resolvedCapture != null) {
          //Might have had resolution issues earlier so might not be set.
          //Not sure about how we will deal with functions passed as variables - maybe use the type.
          var newCapturedSymbol = symbolFactory.newVariable(variableName, param.start, false, false);
          newCapturedSymbol.setType(resolvedCapture.getType());
          newCapturedSymbol.setInitialisedBy(resolvedCapture.getInitialisedBy());
          scope.define(newCapturedSymbol);
        }
      });
    }
  }
}

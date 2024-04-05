package org.ek9lang.compiler.phase1;

import java.util.HashMap;
import java.util.function.Consumer;
import org.antlr.v4.runtime.Token;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;

/**
 * Dynamic Variables when captured have to either be identifiers in their own right,
 * but if they are some type of expression, method call, etc. Then they must use a parameter name.
 * This is so when the dynamic method/class is created it is possible for the compiler to
 * declare the properties/fields to align with those being captured.
 */
final class CheckDynamicVariableCapture implements Consumer<EK9Parser.DynamicVariableCaptureContext> {
  private final ErrorListener errorListener;

  CheckDynamicVariableCapture(final ErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  @Override
  public void accept(final EK9Parser.DynamicVariableCaptureContext ctx) {
    //Now as a very quick an early check, it is possible to detect duplicated names being
    //employed in a dynamic variable capture, these can be named parameters or just the identifier

    if (ctx.paramExpression().expressionParam() != null) {
      checkParamExpression(ctx.paramExpression());
    }

  }

  private void checkParamExpression(final EK9Parser.ParamExpressionContext ctx) {

    final var checkValues = new HashMap<String, Token>();

    for (var param : ctx.expressionParam()) {
      if (!raiseErrorIfParameterNeedsNaming(param)) {
        //Looks like it could be OK
        String identifierName = param.identifier() != null
            ? param.identifier().getText()
            : param.expression().primary().identifierReference().getText();

        final var existing = checkValues.get(identifierName);
        if (existing == null) {
          checkValues.put(identifierName, param.start);
        } else {
          //We've already got a variable/field/property that has than name
          //Then the dynamic function/class would end up with multiple fields of the same name.
          final var msg = "and '" + existing.getText() + "' on line " + existing.getLine();
          errorListener.semanticError(param.expression().start, msg,
              ErrorListener.SemanticClassification.DUPLICATE_VARIABLE_IN_CAPTURE);
        }
      }
    }
  }

  private boolean raiseErrorIfParameterNeedsNaming(final EK9Parser.ExpressionParamContext param) {

    if ((param.expression().primary() == null
        || param.expression().primary().identifierReference() == null)
        && param.identifier() == null) {
      //Developer has not used a simple identifier, but also has not named a parameter to use
      errorListener.semanticError(param.expression().start, "",
          ErrorListener.SemanticClassification.CAPTURED_VARIABLE_MUST_BE_NAMED);
      return true;
    }

    return false;

  }
}

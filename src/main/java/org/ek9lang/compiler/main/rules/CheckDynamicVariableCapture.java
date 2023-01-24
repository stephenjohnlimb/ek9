package org.ek9lang.compiler.main.rules;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.errors.ErrorListener;

/**
 * Dynamic Variables when captured have to either be identifiers in their own right,
 * but if they are some type of expression, method call, etc. Then they must use a parameter name.
 * This is so when the dynamic method/class is created it is possible for the compiler to
 * declare the properties/fields to align with those being captured.
 */
public class CheckDynamicVariableCapture implements Consumer<EK9Parser.DynamicVariableCaptureContext> {
  private final ErrorListener errorListener;

  public CheckDynamicVariableCapture(final ErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  @Override
  public void accept(EK9Parser.DynamicVariableCaptureContext ctx) {
    if (ctx.paramExpression().expressionParam() != null) {
      for (var param : ctx.paramExpression().expressionParam()) {
        if ((param.expression().primary() == null
            || param.expression().primary().identifierReference() == null)
            && param.identifier() == null) {
          //Developer has not used a simple identifier, but also hase not named a parameter to use
          errorListener.semanticError(param.start, "",
              ErrorListener.SemanticClassification.CAPTURED_VARIABLE_MUST_BE_NAMED);
        }
      }
      //So there are some parameters
    }
  }
}

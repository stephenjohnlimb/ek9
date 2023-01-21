package org.ek9lang.compiler.main.rules;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.errors.ErrorListener;

/**
 * Checks on whether the '?' can be used as a modifier
 * then check the parent of this context. Error if developer has; must state that this variable can be
 * uninitialised.
 * For incoming parameters it is not appropriate to use '!', '?'.
 * For a returning parameter it is not appropriate to use '!' i.e. injection nor a web correlation.
 * For aggregate properties and block statements one of ! or ? is needed, but web correlation is not allowed.
 */
public class CheckVariableOnlyDeclaration implements Consumer<EK9Parser.VariableOnlyDeclarationContext> {
  private final ErrorListener errorListener;

  public CheckVariableOnlyDeclaration(final ErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  @Override
  public void accept(EK9Parser.VariableOnlyDeclarationContext ctx) {

    if (ctx.getParent() instanceof EK9Parser.ArgumentParamContext) {

      if (ctx.BANG() != null) {
        errorListener.semanticError(ctx.start, "",
            ErrorListener.SemanticClassification.COMPONENT_INJECTION_NOT_POSSIBLE);
      }
      if (ctx.QUESTION() != null) {
        errorListener.semanticError(ctx.start, "", ErrorListener.SemanticClassification.DECLARED_AS_NULL_NOT_NEEDED);
      }

    } else if (ctx.getParent() instanceof EK9Parser.ReturningParamContext) {

      if (ctx.webVariableCorrelation() != null) {
        errorListener.semanticError(ctx.start, "",
            ErrorListener.SemanticClassification.SERVICE_HTTP_ACCESS_NOT_SUPPORTED);
      }
    } else {
      //So for aggregateProperty and Block statements
      if (ctx.webVariableCorrelation() != null) {
        errorListener.semanticError(ctx.start, "",
            ErrorListener.SemanticClassification.SERVICE_HTTP_ACCESS_NOT_SUPPORTED);
      }

      if(ctx.BANG() == null && ctx.QUESTION() == null) {
        //Then we have a variable only declaration that is not initialised.
        //Developer must use injection or must acknowledge this is uninitialised.
        errorListener.semanticError(ctx.start, "variable",
            ErrorListener.SemanticClassification.NOT_INITIALISED_IN_ANY_WAY);
      }

    }
  }
}
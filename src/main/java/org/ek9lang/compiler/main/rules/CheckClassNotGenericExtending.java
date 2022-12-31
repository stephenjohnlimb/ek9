package org.ek9lang.compiler.main.rules;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.errors.ErrorListener;

/**
 * Ensures that a class that is generic in nature does not extend anything else.
 */
public class CheckClassNotGenericExtending implements Consumer<EK9Parser.ClassDeclarationContext> {

  private final ErrorListener errorListener;

  public CheckClassNotGenericExtending(final ErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  @Override
  public void accept(EK9Parser.ClassDeclarationContext ctx) {

    if (ctx.parameterisedParams() != null && ctx.extendDeclaration() != null) {
      errorListener.semanticError(ctx.start, "",
          ErrorListener.SemanticClassification.GENERIC_TYPE_DEFINITION_CANNOT_EXTEND);

    }
  }
}

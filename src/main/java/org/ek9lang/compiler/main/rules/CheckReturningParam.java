package org.ek9lang.compiler.main.rules;

import java.util.function.BiConsumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.symbol.VariableSymbol;

/**
 * Limits the use of '<- rtn <- SomeCall()' for returning so that a typeDef has to be provided.
 * This helps with type resolution - else we end up chasing types everywhere.
 */
public class CheckReturningParam implements BiConsumer<EK9Parser.ReturningParamContext, VariableSymbol> {

  private final ErrorListener errorListener;

  public CheckReturningParam(final ErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  @Override
  public void accept(final EK9Parser.ReturningParamContext ctx, final VariableSymbol variableSymbol) {
    if (ctx.variableDeclaration() != null && ctx.variableDeclaration().LEFT_ARROW() != null
        && variableSymbol.getType().isEmpty()) {

      //While the grammar supports this, we currently don't in the compiler. Make type resolution much harder.
      //If types can be resolved in phase1 - simple built in types, then that's fine, but we can't 'chase the type'.
      errorListener.semanticError(ctx.LEFT_ARROW().getSymbol(), "use '<- " + variableSymbol.getName() + " as {type}?'",
          ErrorListener.SemanticClassification.ONLY_SIMPLE_RETURNING_TYPES_SUPPORTED);
    }
  }
}

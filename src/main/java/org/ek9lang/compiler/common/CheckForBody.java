package org.ek9lang.compiler.common;

import java.util.function.Predicate;
import org.ek9lang.antlr.EK9Parser;

/**
 * Checks in the context to see if a variableDeclaration or a body is defined.
 * If that is the case then there will be some processing body to the function/method.
 * Other checks will ensure that the return value (if present is set).
 */
public class CheckForBody implements Predicate<EK9Parser.OperationDetailsContext> {

  @Override
  public boolean test(final EK9Parser.OperationDetailsContext ctx) {

    if (ctx == null) {
      return false;
    }

    if (ctx.instructionBlock() != null) {
      return true;
    }

    return ctx.returningParam() != null && ctx.returningParam().variableDeclaration() != null;
  }
}

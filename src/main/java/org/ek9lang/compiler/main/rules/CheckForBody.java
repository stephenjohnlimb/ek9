package org.ek9lang.compiler.main.rules;

import java.util.function.Predicate;
import org.ek9lang.antlr.EK9Parser;

/**
 * Checks in the context to see if a variableDeclaration or a body is defined.
 * If that is the case then there will be some processing body to the function/method.
 * Other checks will ensure that the return value (if present is set).
 */
public class CheckForBody implements Predicate<EK9Parser.MethodDeclarationContext> {
  @Override
  public boolean test(EK9Parser.MethodDeclarationContext ctx) {
    var returningVariableCtxPresent = false;
    var returningCtxPresent = false;
    var instructionBlockCtxPresent = false;

    if (ctx.operationDetails() != null) {
      instructionBlockCtxPresent = ctx.operationDetails().instructionBlock() != null;
      returningCtxPresent = ctx.operationDetails().returningParam() != null;
      if (returningCtxPresent) {
        returningVariableCtxPresent = ctx.operationDetails().returningParam().variableDeclaration() != null;
      }
    }

    return returningVariableCtxPresent || instructionBlockCtxPresent;
  }
}

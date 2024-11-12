package org.ek9lang.compiler.phase1;

import java.util.function.BiConsumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolsAndScopes;

/**
 * Checks that if a return is used (i.e. as parent context is part of an expression), then if a preflow is employed
 * that the preflow is NOT a guard.
 * The reason for this is to prevent the lhs of an expression resulting in being uninitialized should the 'guard'
 * trigger the prevention of the whole expression on the rhs not being executed.
 * So, for normal statements using while, for, try, switch the guard can be useful and stop lots of checks and wasted
 * processing. But if this same approach is allowed with the expression form, it means that we have a dandling and
 * uninitialised lhs variable. This is a bigger problem in a 'pure' scope block and we always drive to ensure variables
 * are only assigned to once (except for returns and other very small cases (loop variables etc).
 */
class ValidPreFlowAndReturnOrError extends RuleSupport
    implements BiConsumer<EK9Parser.PreFlowStatementContext, EK9Parser.ReturningParamContext> {
  ValidPreFlowAndReturnOrError(final SymbolsAndScopes symbolsAndScopes,
                               final ErrorListener errorListener) {
    super(symbolsAndScopes, errorListener);
  }

  @Override
  public void accept(final EK9Parser.PreFlowStatementContext preFlowStatementContext,
                     final EK9Parser.ReturningParamContext returningParamContext) {

    //So not used as an expression with a returning value, so a guard of any form can be used.
    if (returningParamContext == null) {
      return;
    }

    //There is no preflow part at all, so that is also fine, preflow is an optional part.
    if (preFlowStatementContext == null) {
      return;
    }

    //THis too is fine other types of preflow always result in the block executing, only guard makes it conditional.
    if (preFlowStatementContext.guardExpression() == null) {
      return;
    }

    errorListener.semanticError(preFlowStatementContext.guardExpression().op, "",
        ErrorListener.SemanticClassification.GUARD_USED_IN_EXPRESSION);

  }
}

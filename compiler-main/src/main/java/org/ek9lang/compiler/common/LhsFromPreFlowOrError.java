package org.ek9lang.compiler.common;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.PRE_FLOW_SYMBOL_NOT_RESOLVED;

import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.core.CompilerException;

/**
 * Access the Symbol for the lhs of the preflow statement.
 * i.e. what is the subject of the preflow.
 * Now this can vary quite widely, because we have declarations, assignments and guards.
 * <pre>
 *   preFlowStatement
 *     : (variableDeclaration | assignmentStatement | guardExpression) (WITH|THEN)?
 *     ;
 * </pre>
 * <p>
 *   While this is in the common package, it does expect symbols to be defined and 'typed'.
 *   So use after phase three onwards.
 * </p>
 */
public final class LhsFromPreFlowOrError extends TypedSymbolAccess
    implements Function<EK9Parser.PreFlowStatementContext, ISymbol> {

  public LhsFromPreFlowOrError(final SymbolsAndScopes symbolsAndScopes,
                               final ErrorListener errorListener) {
    super(symbolsAndScopes, errorListener);
  }

  @Override
  public ISymbol apply(final EK9Parser.PreFlowStatementContext ctx) {
    if (ctx.variableDeclaration() != null) {
      return getRecordedAndTypedSymbol(ctx.variableDeclaration().identifier());
    } else if (ctx.assignmentStatement() != null) {
      if (ctx.assignmentStatement().identifier() != null) {
        return getRecordedAndTypedSymbol(ctx.assignmentStatement().identifier());
      } else {
        errorListener.semanticError(ctx.start, "Only simply variable assignments supported",
            PRE_FLOW_SYMBOL_NOT_RESOLVED);
        return null;
      }
    } else if (ctx.guardExpression() != null) {
      return getRecordedAndTypedSymbol(ctx.guardExpression().identifier());
    }
    throw new CompilerException("Unexpected condition encountered [" + ctx.getText() + "]");
  }
}

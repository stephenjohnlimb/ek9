package org.ek9lang.compiler.phase3;

import static org.ek9lang.compiler.support.SymbolFactory.NO_REFERENCED_RESET;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.search.SymbolSearch;
import org.ek9lang.compiler.symbols.WhileSymbol;

/**
 * Deals with checking the 'while' or 'do/while control variable in the statement/expression.
 * There is a check on an identifier for 'ProcessIdentifierAssignment' an assignment.
 * This resets the 'referenced' flag on the variable. So for loop variables we must
 * make a note that this is a loop variable to stop this resetting. Otherwise, it looks
 * like the variable is not checked 'after' assignment.
 */
final class ProcessWhileControlVariable extends TypedSymbolAccess
    implements Consumer<EK9Parser.WhileStatementExpressionContext> {

  ProcessWhileControlVariable(SymbolsAndScopes symbolsAndScopes,
                              ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);

  }

  @Override
  public void accept(final EK9Parser.WhileStatementExpressionContext ctx) {

    final var whileSymbol = (WhileSymbol) symbolsAndScopes.getRecordedSymbol(ctx);
    if (ctx.control != null) {
      markAnyIdentifierAsLoopAccessed(whileSymbol, ctx.control);
    }

  }

  /**
   * This is slightly recursive because expressions can employ expressions.
   */
  private void markAnyIdentifierAsLoopAccessed(final WhileSymbol whileSymbol, final EK9Parser.ExpressionContext ctx) {

    if (ctx.expression() != null && !ctx.expression().isEmpty()) {
      ctx.expression().forEach(expressionCtx -> markAnyIdentifierAsLoopAccessed(whileSymbol, expressionCtx));
    }

    if (ctx.left != null) {
      markAnyIdentifierAsLoopAccessed(whileSymbol, ctx.left);
    }

    if (ctx.right != null) {
      markAnyIdentifierAsLoopAccessed(whileSymbol, ctx.right);
    }

    if (ctx.primary() != null && ctx.primary().identifierReference() != null) {
      //Because this is in the entry to 'while' the identifier reference wil not yet have been resolved
      //and associated with the appropriate ctx. Hence, it is necessary to resolve here.
      final var resolved = whileSymbol.resolve(new SymbolSearch(ctx.primary().identifierReference().getText()));
      resolved.ifPresent(variable -> variable.putSquirrelledData(NO_REFERENCED_RESET, "TRUE"));
    }

  }
}

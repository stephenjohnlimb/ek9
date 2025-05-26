package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.symbols.WhileSymbol;

/**
 * Deals with checking the 'while' or 'do/while statement/expression.
 */
final class WhileStatementExpressionOrError extends TypedSymbolAccess
    implements Consumer<EK9Parser.WhileStatementExpressionContext> {
  private final SetTypeFromReturningParam setTypeFromReturningParam;
  private final ControlIsBooleanOrError controlIsBooleanOrError;

  WhileStatementExpressionOrError(final SymbolsAndScopes symbolsAndScopes,
                                  final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);
    this.setTypeFromReturningParam = new SetTypeFromReturningParam(symbolsAndScopes, errorListener);
    this.controlIsBooleanOrError = new ControlIsBooleanOrError(symbolsAndScopes, errorListener);

  }

  @Override
  public void accept(final EK9Parser.WhileStatementExpressionContext ctx) {

    if (symbolsAndScopes.getRecordedSymbol(ctx) instanceof WhileSymbol whileExpression) {
      setTypeFromReturningParam.accept(whileExpression, ctx.returningParam());
      controlIsBooleanOrError.accept(ctx.control);
    }

  }

}

package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.symbols.WhileSymbol;

/**
 * Deals with checking the 'while' or 'do/while statement/expression.
 */
final class ProcessWhileStatementExpression extends TypedSymbolAccess
    implements Consumer<EK9Parser.WhileStatementExpressionContext> {
  private final SetTypeFromReturningParam setTypeFromReturningParam;
  private final CheckControlIsBooleanOrError checkControlIsBooleanOrError;

  ProcessWhileStatementExpression(SymbolAndScopeManagement symbolAndScopeManagement,
                                  ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
    this.setTypeFromReturningParam = new SetTypeFromReturningParam(symbolAndScopeManagement, errorListener);
    this.checkControlIsBooleanOrError = new CheckControlIsBooleanOrError(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(final EK9Parser.WhileStatementExpressionContext ctx) {

    var whileExpression = (WhileSymbol) symbolAndScopeManagement.getRecordedSymbol(ctx);
    setTypeFromReturningParam.accept(whileExpression, ctx.returningParam());
    checkControlIsBooleanOrError.accept(ctx.control);
  }

}

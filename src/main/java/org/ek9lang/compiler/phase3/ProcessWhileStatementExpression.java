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
final class ProcessWhileStatementExpression extends TypedSymbolAccess
    implements Consumer<EK9Parser.WhileStatementExpressionContext> {
  private final SetTypeFromReturningParam setTypeFromReturningParam;
  private final CheckControlIsBooleanOrError checkControlIsBooleanOrError;

  ProcessWhileStatementExpression(final SymbolsAndScopes symbolsAndScopes,
                                  final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);
    this.setTypeFromReturningParam = new SetTypeFromReturningParam(symbolsAndScopes, errorListener);
    this.checkControlIsBooleanOrError = new CheckControlIsBooleanOrError(symbolsAndScopes, errorListener);

  }

  @Override
  public void accept(final EK9Parser.WhileStatementExpressionContext ctx) {

    final var whileExpression = (WhileSymbol) symbolsAndScopes.getRecordedSymbol(ctx);
    setTypeFromReturningParam.accept(whileExpression, ctx.returningParam());
    checkControlIsBooleanOrError.accept(ctx.control);

  }

}

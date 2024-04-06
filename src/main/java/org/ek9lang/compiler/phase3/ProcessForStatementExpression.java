package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.symbols.ForSymbol;

/**
 * Deals with checking the 'for' statement/expression.
 */
final class ProcessForStatementExpression extends TypedSymbolAccess
    implements Consumer<EK9Parser.ForStatementExpressionContext> {
  private final SetTypeFromReturningParam setTypeFromReturningParam;

  ProcessForStatementExpression(final SymbolAndScopeManagement symbolAndScopeManagement,
                                final ErrorListener errorListener) {

    super(symbolAndScopeManagement, errorListener);
    this.setTypeFromReturningParam = new SetTypeFromReturningParam(symbolAndScopeManagement, errorListener);

  }

  @Override
  public void accept(final EK9Parser.ForStatementExpressionContext ctx) {

    final var forExpression = (ForSymbol) symbolAndScopeManagement.getRecordedSymbol(ctx);
    setTypeFromReturningParam.accept(forExpression, ctx.returningParam());

  }

}

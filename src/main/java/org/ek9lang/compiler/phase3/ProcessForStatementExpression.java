package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.symbols.ForSymbol;

/**
 * Deals with checking the 'for' statement/expression.
 */
final class ProcessForStatementExpression extends TypedSymbolAccess
    implements Consumer<EK9Parser.ForStatementExpressionContext> {
  private final SetTypeFromReturningParam setTypeFromReturningParam;

  ProcessForStatementExpression(final SymbolsAndScopes symbolsAndScopes,
                                final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);
    this.setTypeFromReturningParam = new SetTypeFromReturningParam(symbolsAndScopes, errorListener);

  }

  @Override
  public void accept(final EK9Parser.ForStatementExpressionContext ctx) {

    final var forExpression = (ForSymbol) symbolsAndScopes.getRecordedSymbol(ctx);
    setTypeFromReturningParam.accept(forExpression, ctx.returningParam());

  }

}

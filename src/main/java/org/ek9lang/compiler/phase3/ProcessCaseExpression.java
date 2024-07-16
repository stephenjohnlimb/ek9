package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.antlr.v4.runtime.tree.ParseTree;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Just checks and records the appropriate type for the case expression, based on how it is used.
 * This will be important for full switch type checks. Given the operator type in each of the case parts the
 * operator must exit on the type being switched against.
 */
final class ProcessCaseExpression extends TypedSymbolAccess implements Consumer<EK9Parser.CaseExpressionContext> {
  ProcessCaseExpression(SymbolsAndScopes symbolsAndScopes,
                        ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);

  }

  /**
   * Need to ensure each or any of these has a type and record it against this context.
   * <pre>
   * caseExpression
   *     : call
   *     | objectAccessExpression
   *     | op=(LE | GE | GT | LT) expression
   *     | MATCHES expression
   *     | primary
   *     ;
   * </pre>
   */
  @Override
  public void accept(final EK9Parser.CaseExpressionContext ctx) {

    if (ctx.call() != null) {
      recordAgainstContext(getRecordedAndTypedSymbol(ctx.call()), ctx);
    } else if (ctx.objectAccessExpression() != null) {
      recordAgainstContext(getRecordedAndTypedSymbol(ctx.objectAccessExpression()), ctx);
    } else if (ctx.expression() != null) {
      recordAgainstContext(getRecordedAndTypedSymbol(ctx.expression()), ctx);
    } else if (ctx.primary() != null) {
      recordAgainstContext(getRecordedAndTypedSymbol(ctx.primary()), ctx);
    }
  }

  private void recordAgainstContext(final ISymbol symbol, final ParseTree ctx) {

    if (symbol != null) {
      recordATypedSymbol(symbol, ctx);
    }

  }
}

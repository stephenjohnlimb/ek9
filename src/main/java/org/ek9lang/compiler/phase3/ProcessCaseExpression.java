package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Just checks and records the appropriate type for the case expression, based on how it is used.
 * This will be important for full switch type checks. Given the operator type in each of the case parts the
 * operator must exit on the type being switched against.
 */
public class ProcessCaseExpression extends TypedSymbolAccess implements Consumer<EK9Parser.CaseExpressionContext> {
  protected ProcessCaseExpression(SymbolAndScopeManagement symbolAndScopeManagement,
                                  ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
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
    ISymbol symbol = null;
    if (ctx.call() != null) {
      symbol = getRecordedAndTypedSymbol(ctx.call());
    } else if (ctx.objectAccessExpression() != null) {
      symbol = getRecordedAndTypedSymbol(ctx.call());
    } else if (ctx.expression() != null) {
      symbol = getRecordedAndTypedSymbol(ctx.expression());
    } else if (ctx.primary() != null) {
      symbol = getRecordedAndTypedSymbol(ctx.primary());
    }
    //If the symbol is not present or typed an error will have been emitted.
    if (symbol != null) {
      recordATypedSymbol(symbol, ctx);
    }
  }
}

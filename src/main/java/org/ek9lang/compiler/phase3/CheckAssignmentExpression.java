package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.core.AssertValue;

/**
 * Just really pulls up the appropriate symbol from the expression into this context.
 * May add additional check in here in the future.
 */
final class CheckAssignmentExpression extends RuleSupport implements Consumer<EK9Parser.AssignmentExpressionContext> {


  /**
   * Check on references to variables in blocks.
   */
  CheckAssignmentExpression(final SymbolAndScopeManagement symbolAndScopeManagement,
                            final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(final EK9Parser.AssignmentExpressionContext ctx) {
    var symbol = determineSymbolToRecord(ctx);
    if (symbol != null) {
      symbolAndScopeManagement.recordSymbol(symbol, ctx);
    }
  }

  private ISymbol determineSymbolToRecord(final EK9Parser.AssignmentExpressionContext ctx) {
    ISymbol symbol = null;
    if (ctx.expression() != null) {
      symbol = symbolAndScopeManagement.getRecordedSymbol(ctx.expression());
    } else if (ctx.switchStatementExpression() != null) {
      symbol = symbolAndScopeManagement.getRecordedSymbol(ctx.switchStatementExpression());
    } else if (ctx.tryStatementExpression() != null) {
      symbol = symbolAndScopeManagement.getRecordedSymbol(ctx.tryStatementExpression());
    } else if (ctx.dynamicClassDeclaration() != null) {
      symbol = symbolAndScopeManagement.getRecordedSymbol(ctx.dynamicClassDeclaration());
    } else if (ctx.stream() != null) {
      symbol = symbolAndScopeManagement.getRecordedSymbol(ctx.stream());
    } else {
      AssertValue.fail("Expecting finite set of operations for assignment expression");
    }
    return symbol;
  }
}

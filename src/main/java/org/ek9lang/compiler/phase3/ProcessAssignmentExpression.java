package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.core.AssertValue;

/**
 * Just really pulls up the appropriate symbol from the expression into this context.
 * May add additional check in here in the future.
 */
final class ProcessAssignmentExpression extends TypedSymbolAccess
    implements Consumer<EK9Parser.AssignmentExpressionContext> {


  /**
   * Check on references to variables in blocks.
   */
  ProcessAssignmentExpression(final SymbolAndScopeManagement symbolAndScopeManagement,
                              final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(final EK9Parser.AssignmentExpressionContext ctx) {
    var symbol = doProcess(ctx);
    if (symbol != null) {
      recordATypedSymbol(symbol, ctx);
      symbol.getType().ifPresent(type -> {
        if (type.isExactSameType(symbolAndScopeManagement.getEk9Types().ek9Void())) {
          errorListener.semanticError(ctx.start, "",
              ErrorListener.SemanticClassification.RETURN_TYPE_VOID_MEANINGLESS);
        }
      });
    }
  }

  private ISymbol doProcess(final EK9Parser.AssignmentExpressionContext ctx) {
    ISymbol symbol = null;
    if (ctx.expression() != null) {
      symbol = getRecordedAndTypedSymbol(ctx.expression());
    } else if (ctx.switchStatementExpression() != null) {
      symbol = getRecordedAndTypedSymbol(ctx.switchStatementExpression());
    } else if (ctx.tryStatementExpression() != null) {
      symbol = getRecordedAndTypedSymbol(ctx.tryStatementExpression());
    } else if (ctx.dynamicClassDeclaration() != null) {
      symbol = getRecordedAndTypedSymbol(ctx.dynamicClassDeclaration());
    } else if (ctx.stream() != null) {
      symbol = getRecordedAndTypedSymbol(ctx.stream());
    } else if (ctx.guardExpression() != null) {
      symbol = getRecordedAndTypedSymbol(ctx.guardExpression());
    } else {
      AssertValue.fail("Expecting finite set of operations for assignment expression");
    }
    return symbol;
  }
}

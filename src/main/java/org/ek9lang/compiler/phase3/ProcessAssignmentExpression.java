package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
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
  ProcessAssignmentExpression(final SymbolsAndScopes symbolsAndScopes,
                              final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);

  }

  @Override
  public void accept(final EK9Parser.AssignmentExpressionContext ctx) {

    final var symbol = doProcess(ctx);
    if (symbol != null) {
      recordATypedSymbol(symbol, ctx);
      symbol.getType().ifPresent(type -> {
        if (type.isExactSameType(symbolsAndScopes.getEk9Types().ek9Void())) {
          errorListener.semanticError(ctx.start, "",
              ErrorListener.SemanticClassification.RETURN_TYPE_VOID_MEANINGLESS);
        }
      });
    }
  }

  private ISymbol doProcess(final EK9Parser.AssignmentExpressionContext ctx) {

    if (ctx.expression() != null) {
      return getRecordedAndTypedSymbol(ctx.expression());
    } else if (ctx.guardExpression() != null) {
      return getRecordedAndTypedSymbol(ctx.guardExpression());
    } else if (ctx.dynamicClassDeclaration() != null) {
      return getRecordedAndTypedSymbol(ctx.dynamicClassDeclaration());
    } else if (ctx.switchStatementExpression() != null) {
      return getRecordedAndTypedSymbol(ctx.switchStatementExpression());
    } else if (ctx.tryStatementExpression() != null) {
      return getRecordedAndTypedSymbol(ctx.tryStatementExpression());
    } else if (ctx.whileStatementExpression() != null) {
      return getRecordedAndTypedSymbol(ctx.whileStatementExpression());
    } else if (ctx.forStatementExpression() != null) {
      return getRecordedAndTypedSymbol(ctx.forStatementExpression());
    } else if (ctx.streamExpression() != null) {
      return getRecordedAndTypedSymbol(ctx.streamExpression());
    } else {
      AssertValue.fail("Expecting finite set of operations for assignment expression");
    }

    return null;
  }
}

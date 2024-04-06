package org.ek9lang.compiler.phase3;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.SINGLE_EXCEPTION_ONLY;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.symbols.TrySymbol;

/**
 * Deals with checking the try statement/expression.
 */
final class ProcessTryStatementExpression extends TypedSymbolAccess
    implements Consumer<EK9Parser.TryStatementExpressionContext> {
  private final SetTypeFromReturningParam setTypeFromReturningParam;
  private final CheckExceptionTypeOrError checkExceptionTypeOrError;

  ProcessTryStatementExpression(final SymbolAndScopeManagement symbolAndScopeManagement,
                                final ErrorListener errorListener) {

    super(symbolAndScopeManagement, errorListener);
    this.setTypeFromReturningParam = new SetTypeFromReturningParam(symbolAndScopeManagement, errorListener);
    this.checkExceptionTypeOrError = new CheckExceptionTypeOrError(symbolAndScopeManagement, errorListener);

  }

  @Override
  public void accept(final EK9Parser.TryStatementExpressionContext ctx) {

    final var tryExpression = (TrySymbol) symbolAndScopeManagement.getRecordedSymbol(ctx);
    setTypeFromReturningParam.accept(tryExpression, ctx.returningParam());

    checkCatchBlock(ctx);
  }

  private void checkCatchBlock(final EK9Parser.TryStatementExpressionContext ctx) {

    if (ctx.catchStatementExpression() != null) {
      if (ctx.catchStatementExpression().argumentParam().variableOnlyDeclaration().size() != 1) {
        errorListener.semanticError(ctx.catchStatementExpression().argumentParam().RIGHT_ARROW().getSymbol(), "",
            SINGLE_EXCEPTION_ONLY);

      } else {
        final var catchVariable =
            getRecordedAndTypedSymbol(ctx.catchStatementExpression().argumentParam().variableOnlyDeclaration(0));
        checkExceptionTypeOrError.accept(catchVariable.getSourceToken(), catchVariable);
      }
    }
  }

}

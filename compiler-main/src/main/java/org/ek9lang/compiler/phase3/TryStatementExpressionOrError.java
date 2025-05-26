package org.ek9lang.compiler.phase3;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.SINGLE_EXCEPTION_ONLY;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.symbols.TrySymbol;

/**
 * Deals with checking the try statement/expression.
 */
final class TryStatementExpressionOrError extends TypedSymbolAccess
    implements Consumer<EK9Parser.TryStatementExpressionContext> {
  private final SetTypeFromReturningParam setTypeFromReturningParam;
  private final ExceptionTypeOrError exceptionTypeOrError;

  TryStatementExpressionOrError(final SymbolsAndScopes symbolsAndScopes,
                                final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);
    this.setTypeFromReturningParam = new SetTypeFromReturningParam(symbolsAndScopes, errorListener);
    this.exceptionTypeOrError = new ExceptionTypeOrError(symbolsAndScopes, errorListener);

  }

  @Override
  public void accept(final EK9Parser.TryStatementExpressionContext ctx) {

    if (symbolsAndScopes.getRecordedSymbol(ctx) instanceof TrySymbol trySymbol) {
      setTypeFromReturningParam.accept(trySymbol, ctx.returningParam());
    }

    if (ctx.catchStatementExpression() != null
        && ctx.catchStatementExpression().argumentParam() != null
        && ctx.catchStatementExpression().argumentParam().variableOnlyDeclaration() != null) {
      catchBlockOrError(ctx);
    }

  }

  /**
   * It's not mandatory to have a catch block, but if there is one, the must only be ONE.
   * Then that catch block has to be validated.
   */
  private void catchBlockOrError(final EK9Parser.TryStatementExpressionContext ctx) {

    if (ctx.catchStatementExpression().argumentParam().variableOnlyDeclaration().size() != 1) {
      errorListener.semanticError(ctx.catchStatementExpression().argumentParam().RIGHT_ARROW().getSymbol(), "",
          SINGLE_EXCEPTION_ONLY);

    } else {
      final var catchVariable =
          getRecordedAndTypedSymbol(ctx.catchStatementExpression().argumentParam().variableOnlyDeclaration(0));
      exceptionTypeOrError.accept(catchVariable.getSourceToken(), catchVariable);
    }

  }

}

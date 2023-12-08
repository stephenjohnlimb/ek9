package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.tokenizer.Ek9Token;

/**
 * Checks that the throw statement actually throws and Exception type.
 */
final class CheckThrowStatementOrError extends TypedSymbolAccess implements Consumer<EK9Parser.ThrowStatementContext> {
  private final CheckExceptionTypeOrError checkExceptionTypeOrError;

  CheckThrowStatementOrError(final SymbolAndScopeManagement symbolAndScopeManagement,
                             final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
    this.checkExceptionTypeOrError = new CheckExceptionTypeOrError(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(final EK9Parser.ThrowStatementContext ctx) {
    var errorLocation = new Ek9Token(ctx.THROW().getSymbol());
    if (ctx.identifierReference() != null) {
      checkExceptionTypeOrError.accept(errorLocation,
          symbolAndScopeManagement.getRecordedSymbol(ctx.identifierReference()));

    } else if (ctx.call() != null) {
      checkExceptionTypeOrError.accept(errorLocation,
          symbolAndScopeManagement.getRecordedSymbol(ctx.call()));
    }
  }
}

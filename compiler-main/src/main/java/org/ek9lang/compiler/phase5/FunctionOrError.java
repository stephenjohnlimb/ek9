package org.ek9lang.compiler.phase5;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.symbols.FunctionSymbol;

/**
 * Typically checks any returning values to see if they have now been initialised on a function.
 */
final class FunctionOrError extends TypedSymbolAccess
    implements Consumer<EK9Parser.FunctionDeclarationContext> {

  private final ReturningVariableOrError returningVariableOrError;

  FunctionOrError(final SymbolsAndScopes symbolsAndScopes,
                  final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);
    this.returningVariableOrError = new ReturningVariableOrError(symbolsAndScopes, errorListener);

  }

  @Override
  public void accept(final EK9Parser.FunctionDeclarationContext ctx) {

    if (symbolsAndScopes.getRecordedSymbol(ctx) instanceof FunctionSymbol function
        && !function.isMarkedAbstract() && ctx.operationDetails() != null
        && ctx.operationDetails().returningParam() != null) {
      final var scope = symbolsAndScopes.getRecordedScope(ctx);
      returningVariableOrError.accept(scope, ctx.operationDetails());
    }

  }
}

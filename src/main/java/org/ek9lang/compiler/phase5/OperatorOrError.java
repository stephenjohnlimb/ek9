package org.ek9lang.compiler.phase5;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.symbols.MethodSymbol;

/**
 * Typically checks any returning values to see if they have now been initialised on an operator.
 */
final class OperatorOrError extends TypedSymbolAccess
    implements Consumer<EK9Parser.OperatorDeclarationContext> {

  private final ReturningVariableOrError returningVariableOrError;

  OperatorOrError(final SymbolsAndScopes symbolsAndScopes,
                  final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);
    this.returningVariableOrError = new ReturningVariableOrError(symbolsAndScopes, errorListener);

  }

  @Override
  public void accept(final EK9Parser.OperatorDeclarationContext ctx) {

    if (symbolsAndScopes.getRecordedSymbol(ctx) instanceof MethodSymbol method
        && !method.isMarkedAbstract() && ctx.operationDetails() != null
        && ctx.operationDetails().returningParam() != null) {
      final var scope = symbolsAndScopes.getRecordedScope(ctx);
      returningVariableOrError.accept(scope, ctx.operationDetails());
    }

  }
}

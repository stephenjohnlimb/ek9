package org.ek9lang.compiler.phase5;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.RETURN_NOT_ALWAYS_INITIALISED;

import java.util.function.BiConsumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.symbols.IScope;
import org.ek9lang.compiler.symbols.ISymbol;

final class ReturningVariableOrError extends TypedSymbolAccess
    implements BiConsumer<IScope, EK9Parser.OperationDetailsContext> {
  ReturningVariableOrError(final SymbolsAndScopes symbolsAndScopes,
                           final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);

  }

  @Override
  public void accept(final IScope mainScope, final EK9Parser.OperationDetailsContext ctx) {

    final var returningVariables = symbolsAndScopes.getUninitialisedVariables(mainScope).stream()
        .filter(ISymbol::isReturningParameter).toList();

    returningVariables.forEach(variable -> {
      if (ctx.instructionBlock() != null) {
        updateReturningSymbol(ctx, variable, mainScope);
      }
      initialisedOrError(ctx.returningParam(), variable, mainScope);
    });

  }

  private void updateReturningSymbol(final EK9Parser.OperationDetailsContext ctx,
                                     final ISymbol variable,
                                     final IScope scope) {

    final var instructionsScope = symbolsAndScopes.getRecordedScope(ctx.instructionBlock());
    //So now we're at the end of the instruction processing lets see if the return variable was set by the end.
    //Remember only exceptions can cause early return and then the return value is not used.
    if (symbolsAndScopes.isVariableInitialised(variable, instructionsScope)) {
      symbolsAndScopes.markSymbolAsInitialised(variable, scope);
    }

  }

  private void initialisedOrError(final EK9Parser.ReturningParamContext ctx,
                                  final ISymbol variable,
                                  final IScope scope) {

    if (!symbolsAndScopes.isVariableInitialised(variable, scope)) {
      errorListener.semanticError(ctx.LEFT_ARROW().getSymbol(),
          "'" + variable.getName() + "':", RETURN_NOT_ALWAYS_INITIALISED);
    }
  }

}

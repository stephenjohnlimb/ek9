package org.ek9lang.compiler.phase5;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.RETURN_NOT_ALWAYS_INITIALISED;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.symbols.FunctionSymbol;
import org.ek9lang.compiler.symbols.IScope;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Deals with handling any returning values in a dynamic function.
 * This is on exiting the dynamic function declaration - so here it is necessary to check the
 * returning symbol.
 */
final class DynamicFunctionOrError extends TypedSymbolAccess
    implements Consumer<EK9Parser.DynamicFunctionDeclarationContext> {
  DynamicFunctionOrError(final SymbolsAndScopes symbolsAndScopes,
                         final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);

  }

  @Override
  public void accept(final EK9Parser.DynamicFunctionDeclarationContext ctx) {

    final var functionSymbol = (FunctionSymbol) symbolsAndScopes.getRecordedSymbol(ctx);
    final var returningVariables = symbolsAndScopes.getUninitialisedVariables(functionSymbol).stream()
        .filter(ISymbol::isReturningParameter).toList();

    returningVariables.forEach(variable -> {
      if (ctx.dynamicFunctionBody() != null && ctx.dynamicFunctionBody().singleStatementBlock() != null) {
        final var instructionsScope =
            symbolsAndScopes.getRecordedScope(ctx.dynamicFunctionBody().singleStatementBlock());
        updateReturningSymbol(variable, functionSymbol, instructionsScope);
      } else if (ctx.dynamicFunctionBody() != null && ctx.dynamicFunctionBody().block().instructionBlock() != null) {
        final var instructionsScope =
            symbolsAndScopes.getRecordedScope(ctx.dynamicFunctionBody().block().instructionBlock());
        updateReturningSymbol(variable, functionSymbol, instructionsScope);
      }

      initialisedOrError(ctx, variable, functionSymbol);
    });

  }

  private void updateReturningSymbol(final ISymbol variable,
                                     final IScope functionScope,
                                     final IScope instructionsScope) {


    //So now we're at the end of the instruction processing lets see if the return variable was set by the end.
    //Remember only exceptions can cause early return and then the return value is not used.
    if (symbolsAndScopes.isVariableInitialised(variable, instructionsScope)) {
      symbolsAndScopes.markSymbolAsInitialised(variable, functionScope);
    }

  }

  private void initialisedOrError(final EK9Parser.DynamicFunctionDeclarationContext ctx,
                                  final ISymbol variable,
                                  final IScope scope) {

    if (!symbolsAndScopes.isVariableInitialised(variable, scope)) {
      errorListener.semanticError(ctx.start, "'" + variable.getName() + "':", RETURN_NOT_ALWAYS_INITIALISED);
    }

  }
}

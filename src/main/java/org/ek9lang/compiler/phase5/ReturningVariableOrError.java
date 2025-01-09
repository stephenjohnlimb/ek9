package org.ek9lang.compiler.phase5;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.RETURN_NOT_ALWAYS_INITIALISED;

import java.util.function.BiConsumer;
import org.antlr.v4.runtime.tree.ParseTree;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.symbols.IScope;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Checks if return variables are initialised.
 */
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
      //This is OK if there is a block - but what if the return has been set with a single statement?
      if (ctx.instructionBlock() != null) {
        updateReturningSymbol(ctx.instructionBlock(), variable, mainScope);
      }
      if (ctx.returningParam() != null) {
        updateReturningSymbol(ctx.returningParam(), variable, mainScope);
      }
      initialisedOrError(ctx.returningParam(), variable, mainScope);
    });

  }

  private void updateReturningSymbol(final ParseTree node,
                                     final ISymbol variable,
                                     final IScope scope) {

    final var instructionsScope = symbolsAndScopes.getRecordedScope(node);
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

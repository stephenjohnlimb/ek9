package org.ek9lang.compiler.phase5;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.RETURN_NOT_ALWAYS_INITIALISED;

import java.util.function.BiConsumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.phase3.TypedSymbolAccess;
import org.ek9lang.compiler.symbols.IScope;
import org.ek9lang.compiler.symbols.ISymbol;

final class ProcessReturningVariable extends TypedSymbolAccess
    implements BiConsumer<IScope, EK9Parser.OperationDetailsContext> {
  ProcessReturningVariable(final SymbolAndScopeManagement symbolAndScopeManagement,
                           final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(final IScope mainScope, final EK9Parser.OperationDetailsContext ctx) {

    var returningVariables = symbolAndScopeManagement.getUninitialisedVariables(mainScope).stream()
        .filter(ISymbol::isReturningParameter).toList();
    returningVariables.forEach(variable -> {
      if (ctx.instructionBlock() != null) {
        updateReturningSymbol(ctx, variable, mainScope);
      }
      checkInitialisedOrError(ctx.returningParam(), variable, mainScope);
    });
  }

  private void updateReturningSymbol(final EK9Parser.OperationDetailsContext ctx,
                                     final ISymbol variable,
                                     final IScope scope) {

    var instructionsScope = symbolAndScopeManagement.getRecordedScope(ctx.instructionBlock());
    //So now we're at the end of the instruction processing lets see if the return variable was set by the end.
    //Remember only exceptions can cause early return and then the return value is not used.
    if (symbolAndScopeManagement.isVariableInitialised(variable, instructionsScope)) {
      symbolAndScopeManagement.markSymbolAsInitialised(variable, scope);
    }

  }

  private void checkInitialisedOrError(final EK9Parser.ReturningParamContext ctx,
                                       final ISymbol variable,
                                       final IScope scope) {

    if (!symbolAndScopeManagement.isVariableInitialised(variable, scope)) {
      errorListener.semanticError(ctx.LEFT_ARROW().getSymbol(),
          "'" + variable.getName() + "':", RETURN_NOT_ALWAYS_INITIALISED);
    }
  }

}

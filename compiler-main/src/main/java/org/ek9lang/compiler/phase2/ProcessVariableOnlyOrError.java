package org.ek9lang.compiler.phase2;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.support.MostSpecificScope;
import org.ek9lang.compiler.support.NoNameCollisionOrError;

/**
 * Deals with variable only declarations.
 * <p>
 * These can be just properties, parameters and returns.
 * Completes various basic checks.
 * </p>
 */
final class ProcessVariableOnlyOrError extends RuleSupport implements
    Consumer<EK9Parser.VariableOnlyDeclarationContext> {
  private final NotGenericTypeParameterOrError notGenericTypeParameterOrError;
  private final NoNameCollisionOrError noNameCollisionOrError;
  private final MostSpecificScope mostSpecificScope;

  ProcessVariableOnlyOrError(final SymbolsAndScopes symbolsAndScopes,
                             final ErrorListener errorListener) {
    super(symbolsAndScopes, errorListener);
    this.notGenericTypeParameterOrError = new NotGenericTypeParameterOrError(errorListener);
    this.noNameCollisionOrError = new NoNameCollisionOrError(errorListener, false);
    this.mostSpecificScope = new MostSpecificScope(symbolsAndScopes);
  }

  @Override
  public void accept(EK9Parser.VariableOnlyDeclarationContext ctx) {

    final var variableSymbol = symbolsAndScopes.getRecordedSymbol(ctx);

    //Might have been a duplicate and so could be null (other errors emitted).
    if (variableSymbol != null) {
      final var theType = symbolsAndScopes.getRecordedSymbol(ctx.typeDef());
      //Can set the type now if known. Might not be if Parameterized type - not got that far yet.
      if (theType != null) {
        variableSymbol.setType(theType);
      }
      //Do not allow generics to injected.
      if (ctx.BANG() != null) {
        notGenericTypeParameterOrError.accept(variableSymbol);
      }
      //While there is a check in phase one, this causes an ordering issue. So we run this in this phase.
      noNameCollisionOrError.test(mostSpecificScope.get(), variableSymbol);
    }

  }

}

package org.ek9lang.compiler.phase2;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.support.MostSpecificScope;
import org.ek9lang.compiler.support.NoNameCollisionOrError;
import org.ek9lang.compiler.support.SymbolFactory;

final class ProcessVariableOrError extends RuleSupport implements
    Consumer<EK9Parser.VariableDeclarationContext> {
  private final NoNameCollisionOrError noNameCollisionOrError;
  private final MostSpecificScope mostSpecificScope;
  private final ProcessVariableDeclarationOrError processVariableDeclarationOrError;

  ProcessVariableOrError(final SymbolsAndScopes symbolsAndScopes,
                         final SymbolFactory symbolFactory,
                         final ErrorListener errorListener) {
    super(symbolsAndScopes, errorListener);
    this.noNameCollisionOrError =
        new NoNameCollisionOrError(errorListener, false);
    this.mostSpecificScope =
        new MostSpecificScope(symbolsAndScopes);
    this.processVariableDeclarationOrError =
        new ProcessVariableDeclarationOrError(symbolsAndScopes, symbolFactory, errorListener);
  }

  @Override
  public void accept(EK9Parser.VariableDeclarationContext ctx) {
    final var variableSymbol = symbolsAndScopes.getRecordedSymbol(ctx);
    if (variableSymbol != null) {
      if (ctx.typeDef() != null) {
        final var theType = symbolsAndScopes.getRecordedSymbol(ctx.typeDef());
        if (theType != null) {
          variableSymbol.setType(theType);
        }
      } else if (variableSymbol.getType().isEmpty()
          && (variableSymbol.isPropertyField() || variableSymbol.isReturningParameter())
          && ctx.assignmentExpression() != null) {
        processVariableDeclarationOrError.accept(ctx);
      }

      //While there is a check in phase one, this causes an ordering issue. So we run this in this phase.
      noNameCollisionOrError.test(mostSpecificScope.get(), variableSymbol);
    }
  }

}

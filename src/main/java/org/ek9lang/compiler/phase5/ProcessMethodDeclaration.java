package org.ek9lang.compiler.phase5;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.phase3.TypedSymbolAccess;
import org.ek9lang.compiler.symbols.MethodSymbol;

/**
 * Typically checks any returning values to see if they have now been initialised on a method.
 */
final class ProcessMethodDeclaration extends TypedSymbolAccess
    implements Consumer<EK9Parser.MethodDeclarationContext> {

  private final ProcessReturningVariable processReturningVariable;

  ProcessMethodDeclaration(final SymbolAndScopeManagement symbolAndScopeManagement,
                           final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
    this.processReturningVariable = new ProcessReturningVariable(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(final EK9Parser.MethodDeclarationContext ctx) {
    var symbol = symbolAndScopeManagement.getRecordedSymbol(ctx);
    //Note that the method declaration is also used for programs which are aggregates.
    if (symbol instanceof MethodSymbol method && !method.isMarkedAbstract() && ctx.operationDetails() != null
        && ctx.operationDetails().returningParam() != null) {
      var scope = symbolAndScopeManagement.getRecordedScope(ctx);
      processReturningVariable.accept(scope, ctx.operationDetails());
    }

  }
}

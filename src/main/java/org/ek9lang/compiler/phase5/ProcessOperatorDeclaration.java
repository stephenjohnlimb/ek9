package org.ek9lang.compiler.phase5;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.phase3.TypedSymbolAccess;
import org.ek9lang.compiler.symbols.MethodSymbol;

/**
 * Typically checks any returning values to see if they have now been initialised on an operator.
 */
final class ProcessOperatorDeclaration extends TypedSymbolAccess
    implements Consumer<EK9Parser.OperatorDeclarationContext> {

  private final ProcessReturningVariable processReturningVariable;

  ProcessOperatorDeclaration(final SymbolAndScopeManagement symbolAndScopeManagement,
                             final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
    this.processReturningVariable = new ProcessReturningVariable(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(final EK9Parser.OperatorDeclarationContext ctx) {
    var method = (MethodSymbol) symbolAndScopeManagement.getRecordedSymbol(ctx);
    if (!method.isMarkedAbstract() && ctx.operationDetails() != null
        && ctx.operationDetails().returningParam() != null) {
      var scope = symbolAndScopeManagement.getRecordedScope(ctx);
      processReturningVariable.accept(scope, ctx.operationDetails());
    }

  }
}

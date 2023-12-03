package org.ek9lang.compiler.phase5;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.phase3.TypedSymbolAccess;

/**
 * Typically checks any returning values to see if they have now been initialised on a service operation.
 */
final class ProcessServiceOperationDeclaration extends TypedSymbolAccess
    implements Consumer<EK9Parser.ServiceOperationDeclarationContext> {

  private final ProcessReturningVariable processReturningVariable;

  ProcessServiceOperationDeclaration(final SymbolAndScopeManagement symbolAndScopeManagement,
                                     final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
    this.processReturningVariable = new ProcessReturningVariable(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(final EK9Parser.ServiceOperationDeclarationContext ctx) {

    if (ctx.operationDetails() != null && ctx.operationDetails().returningParam() != null) {
      var scope = symbolAndScopeManagement.getRecordedScope(ctx);
      processReturningVariable.accept(scope, ctx.operationDetails());
    }

  }
}

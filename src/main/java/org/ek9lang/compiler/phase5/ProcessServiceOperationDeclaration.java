package org.ek9lang.compiler.phase5;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;

/**
 * Typically checks any returning values to see if they have now been initialised on a service operation.
 */
final class ProcessServiceOperationDeclaration extends TypedSymbolAccess
    implements Consumer<EK9Parser.ServiceOperationDeclarationContext> {

  private final ProcessReturningVariable processReturningVariable;

  ProcessServiceOperationDeclaration(final SymbolsAndScopes symbolsAndScopes,
                                     final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);
    this.processReturningVariable = new ProcessReturningVariable(symbolsAndScopes, errorListener);

  }

  @Override
  public void accept(final EK9Parser.ServiceOperationDeclarationContext ctx) {

    if (ctx.operationDetails() != null && ctx.operationDetails().returningParam() != null) {
      final var scope = symbolsAndScopes.getRecordedScope(ctx);
      processReturningVariable.accept(scope, ctx.operationDetails());
    }

  }
}

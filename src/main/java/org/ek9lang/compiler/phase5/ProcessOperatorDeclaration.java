package org.ek9lang.compiler.phase5;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.symbols.MethodSymbol;

/**
 * Typically checks any returning values to see if they have now been initialised on an operator.
 */
final class ProcessOperatorDeclaration extends TypedSymbolAccess
    implements Consumer<EK9Parser.OperatorDeclarationContext> {

  private final ProcessReturningVariable processReturningVariable;

  ProcessOperatorDeclaration(final SymbolsAndScopes symbolsAndScopes,
                             final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);
    this.processReturningVariable = new ProcessReturningVariable(symbolsAndScopes, errorListener);

  }

  @Override
  public void accept(final EK9Parser.OperatorDeclarationContext ctx) {

    final var method = (MethodSymbol) symbolsAndScopes.getRecordedSymbol(ctx);

    if (!method.isMarkedAbstract() && ctx.operationDetails() != null
        && ctx.operationDetails().returningParam() != null) {
      final var scope = symbolsAndScopes.getRecordedScope(ctx);
      processReturningVariable.accept(scope, ctx.operationDetails());
    }

  }
}

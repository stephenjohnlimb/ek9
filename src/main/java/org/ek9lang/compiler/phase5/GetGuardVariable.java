package org.ek9lang.compiler.phase5;

import java.util.Optional;
import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Extracts the guard variable if there is a guard expression.
 */
final class GetGuardVariable extends TypedSymbolAccess
    implements Function<EK9Parser.PreFlowStatementContext, Optional<ISymbol>> {

  GetGuardVariable(final SymbolsAndScopes symbolsAndScopes,
                   final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);

  }

  @Override
  public Optional<ISymbol> apply(final EK9Parser.PreFlowStatementContext ctx) {

    if (ctx != null && ctx.guardExpression() != null) {
      return Optional.ofNullable(
          symbolsAndScopes.getRecordedSymbol(ctx.guardExpression().identifier()));
    }

    return Optional.empty();
  }
}

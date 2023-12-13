package org.ek9lang.compiler.phase5;

import java.util.Optional;
import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.phase3.TypedSymbolAccess;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Extracts the guard variable if there is a guard expression.
 */
final class GetGuardVariable extends TypedSymbolAccess
    implements Function<EK9Parser.PreFlowStatementContext, Optional<ISymbol>> {
  GetGuardVariable(SymbolAndScopeManagement symbolAndScopeManagement,
                   ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
  }

  @Override
  public Optional<ISymbol> apply(EK9Parser.PreFlowStatementContext ctx) {
    if (ctx != null
        && ctx.guardExpression() != null) {
      return Optional.ofNullable(
          symbolAndScopeManagement.getRecordedSymbol(ctx.guardExpression().identifier()));
    }
    return Optional.empty();
  }
}

package org.ek9lang.compiler.phase3;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Just extracts the ISymbols from the expression contexts and issues not resolved errors if any are not present.
 * So you may not actually get a symbol for every expression. Check the sizing. But there will be semantic errors
 * emitted.
 */
final class SymbolsFromParamExpression extends RuleSupport
    implements Function<EK9Parser.ParamExpressionContext, List<ISymbol>> {

  private final SymbolFromContextOrError symbolFromContextOrError;

  SymbolsFromParamExpression(final SymbolAndScopeManagement symbolAndScopeManagement,
                             final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
    this.symbolFromContextOrError = new SymbolFromContextOrError(symbolAndScopeManagement, errorListener);
  }

  @Override
  public List<ISymbol> apply(EK9Parser.ParamExpressionContext ctx) {
    return ctx.expressionParam()
        .stream()
        .map(EK9Parser.ExpressionParamContext::expression)
        .map(symbolFromContextOrError)
        .filter(Objects::nonNull)
        .toList();
  }
}

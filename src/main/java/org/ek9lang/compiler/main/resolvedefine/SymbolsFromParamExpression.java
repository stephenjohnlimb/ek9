package org.ek9lang.compiler.main.resolvedefine;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.main.phases.definition.SymbolAndScopeManagement;
import org.ek9lang.compiler.symbol.ISymbol;

/**
 * Just extracts the ISymbols from the expression contexts and issues not resolved errors if any are not present.
 * So you may not actually get a symbol for every expression. Check the sizing. But there will be semantic errors
 * emitted.
 */
public class SymbolsFromParamExpression implements Function<EK9Parser.ParamExpressionContext, List<ISymbol>> {

  private final SymbolFromContextOrError symbolFromContextOrError;

  public SymbolsFromParamExpression(final SymbolAndScopeManagement symbolAndScopeManagement,
                                    final ErrorListener errorListener) {
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

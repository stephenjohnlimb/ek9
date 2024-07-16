package org.ek9lang.compiler.phase3;

import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.search.MethodSymbolSearch;

/**
 * Create a MethodSearch symbol for an operation that could have a single expression or two expressions.
 */
final class MethodSymbolSearchForExpression extends TypedSymbolAccess
    implements Function<EK9Parser.ExpressionContext, MethodSymbolSearch> {
  private final OperatorText operatorText = new OperatorText();
  private final SymbolFromContextOrError symbolFromContextOrError;

  MethodSymbolSearchForExpression(
      final SymbolsAndScopes symbolsAndScopes,
      final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);
    this.symbolFromContextOrError = new SymbolFromContextOrError(symbolsAndScopes, errorListener);

  }

  @Override
  public MethodSymbolSearch apply(final EK9Parser.ExpressionContext ctx) {

    //Some operators can have two different 'names' but can only be defined in a single form.
    final var searchMethodName = operatorText.apply(ctx);
    final var search = new MethodSymbolSearch(searchMethodName);

    if (ctx.expression().size() == 2) {
      final var param = symbolFromContextOrError.apply(ctx.expression(1));
      if (param != null && param.getType().isPresent()) {
        search.addTypeParameter(param.getType());
      } else {
        return null;
      }
    }

    return search;
  }
}
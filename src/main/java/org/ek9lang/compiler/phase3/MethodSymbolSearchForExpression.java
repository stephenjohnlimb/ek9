package org.ek9lang.compiler.phase3;

import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.search.MethodSymbolSearch;

/**
 * Create a MethodSearch symbol for an operation that could have a single expression or two expressions.
 */
final class MethodSymbolSearchForExpression extends TypedSymbolAccess
    implements Function<EK9Parser.ExpressionContext, MethodSymbolSearch> {
  private final OperatorText operatorText = new OperatorText();

  MethodSymbolSearchForExpression(
      SymbolAndScopeManagement symbolAndScopeManagement,
      ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
  }

  @Override
  public MethodSymbolSearch apply(EK9Parser.ExpressionContext ctx) {
    //Some operators can have two different 'names' but can only be defined in a single form.
    var searchMethodName = operatorText.apply(ctx);
    var search = new MethodSymbolSearch(searchMethodName);
    if (ctx.expression().size() == 2) {
      var param = getRecordedAndTypedSymbol(ctx.expression(1));
      if (param != null && param.getType().isPresent()) {
        search.addTypeParameter(param.getType());
      } else {
        return null;
      }
    }
    return search;
  }
}

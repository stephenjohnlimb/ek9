package org.ek9lang.compiler.phase3;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.MUST_BE_A_BOOLEAN;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;

/**
 * Just checks that an expression used in a control for if/while etc is a Boolean type.
 * If not emits and error.
 */
final class CheckControlIsBooleanOrError extends TypedSymbolAccess
    implements Consumer<EK9Parser.ExpressionContext> {
  CheckControlIsBooleanOrError(final SymbolAndScopeManagement symbolAndScopeManagement,
                               final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(EK9Parser.ExpressionContext ctx) {

    var controlExpression = getRecordedAndTypedSymbol(ctx);
    if (controlExpression != null) {
      //If was null/untyped then - error will have already been emitted.
      controlExpression.getType().ifPresent(controlType -> {
        if (!controlType.isAssignableTo(symbolAndScopeManagement.getEk9Types().ek9Boolean())) {
          errorListener.semanticError(ctx.start, "'" + controlType.getFriendlyName() + "':", MUST_BE_A_BOOLEAN);
        }
      });

    }
  }
}
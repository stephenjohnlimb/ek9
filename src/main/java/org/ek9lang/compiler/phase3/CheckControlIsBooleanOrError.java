package org.ek9lang.compiler.phase3;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.MUST_BE_A_BOOLEAN;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;

/**
 * Just checks that an expression used in a control for if/while etc is a Boolean type.
 * If not emits and error.
 */
final class CheckControlIsBooleanOrError extends TypedSymbolAccess
    implements Consumer<EK9Parser.ExpressionContext> {
  CheckControlIsBooleanOrError(final SymbolsAndScopes symbolsAndScopes,
                               final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);

  }

  @Override
  public void accept(final EK9Parser.ExpressionContext ctx) {

    final var controlExpression = getRecordedAndTypedSymbol(ctx);

    if (controlExpression != null) {
      //If was null/untyped then - error will have already been emitted.
      controlExpression.getType().ifPresent(controlType -> {
        if (!controlType.isAssignableTo(symbolsAndScopes.getEk9Types().ek9Boolean())) {
          errorListener.semanticError(ctx.start, "'" + controlType.getFriendlyName() + "':", MUST_BE_A_BOOLEAN);
        }
      });

    }
  }
}
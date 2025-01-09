package org.ek9lang.compiler.phase3;

import java.util.Optional;
import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;

/**
 * Just gets both of the ternary expression parts (not the control).
 */
final class AccessTernaryExpressionsOrError extends TypedSymbolAccess
    implements Function<EK9Parser.ExpressionContext, Optional<ExprLeftAndRightData>> {
  private final SymbolFromContextOrError symbolFromContextOrError;

  AccessTernaryExpressionsOrError(final SymbolsAndScopes symbolsAndScopes,
                                  final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);
    this.symbolFromContextOrError = new SymbolFromContextOrError(symbolsAndScopes, errorListener);

  }

  @Override
  public Optional<ExprLeftAndRightData> apply(final EK9Parser.ExpressionContext ctx) {

    final var left = symbolFromContextOrError.apply(ctx.ternaryPart(0).expression());
    final var right = symbolFromContextOrError.apply(ctx.ternaryPart(1).expression());

    //Above will have checked type, but let's issue not resolved if null as well.
    if (left != null && right != null) {
      return Optional.of(new ExprLeftAndRightData(left, right));
    }

    return Optional.empty();
  }
}

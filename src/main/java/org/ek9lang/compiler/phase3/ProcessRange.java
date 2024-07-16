package org.ek9lang.compiler.phase3;

import java.util.List;
import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.support.CommonTypeSuperOrTrait;
import org.ek9lang.compiler.support.SymbolFactory;
import org.ek9lang.compiler.tokenizer.Ek9Token;

/**
 * Checks the Range and ensures that there a Symbol recorded against that context.
 * Then if types are present on the two parts of the range it will check those types
 * and/or issue errors if they are incompatible.
 */
final class ProcessRange extends TypedSymbolAccess implements Consumer<EK9Parser.RangeContext> {
  private final SymbolFactory symbolFactory;
  private final SymbolFromContextOrError symbolFromContextOrError;
  private final CommonTypeSuperOrTrait commonTypeSuperOrTrait;

  /**
   * Check range expressions and record an expression for the type.
   */
  ProcessRange(final SymbolsAndScopes symbolsAndScopes,
               final SymbolFactory symbolFactory,
               final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);
    this.symbolFactory = symbolFactory;
    this.symbolFromContextOrError = new SymbolFromContextOrError(symbolsAndScopes, errorListener);
    this.commonTypeSuperOrTrait = new CommonTypeSuperOrTrait(errorListener);

  }

  @Override
  public void accept(final EK9Parser.RangeContext ctx) {

    final var startToken = new Ek9Token(ctx.start);
    final var rangeExpr = symbolFactory.newExpressionSymbol(startToken, ctx.getText());
    final var fromExpr = symbolFromContextOrError.apply(ctx.expression(0));
    final var toExpr = symbolFromContextOrError.apply(ctx.expression(1));

    if (fromExpr != null && toExpr != null) {
      final var commonType = commonTypeSuperOrTrait.apply(startToken, List.of(fromExpr, toExpr));
      rangeExpr.setType(commonType);
    }

    //Otherwise there would have been unresolved errors earlier.
    //record and if not typed there will be an error emitted.
    recordATypedSymbol(rangeExpr, ctx);

  }
}

package org.ek9lang.compiler.phase3;

import java.util.List;
import java.util.function.Consumer;
import org.antlr.v4.runtime.Token;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.support.CommonTypeSuperOrTrait;
import org.ek9lang.compiler.symbols.support.SymbolFactory;

/**
 * Checks the Range and ensures that there a Symbol recorded against that context.
 * Then if types are present on the two parts of the range it will check those types
 * and/or issue errors if they are incompatible.
 */
final class CheckRange extends RuleSupport implements Consumer<EK9Parser.RangeContext> {

  private final SymbolFactory symbolFactory;

  private final CommonTypeSuperOrTrait commonTypeSuperOrTrait;

  /**
   * Check range expressions and record an expression for the type.
   */
  CheckRange(final SymbolAndScopeManagement symbolAndScopeManagement,
             final SymbolFactory symbolFactory,
             final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
    this.symbolFactory = symbolFactory;

    this.commonTypeSuperOrTrait = new CommonTypeSuperOrTrait(errorListener);
  }

  @Override
  public void accept(EK9Parser.RangeContext ctx) {
    //Add one in and hopefully 'type it'.
    var rangeExpr = symbolFactory.newExpressionSymbol(ctx.start, ctx.getText());
    symbolAndScopeManagement.recordSymbol(rangeExpr, ctx);

    var fromExpr = symbolAndScopeManagement.getRecordedSymbol(ctx.expression(0));
    var toExpr = symbolAndScopeManagement.getRecordedSymbol(ctx.expression(1));

    if (fromExpr != null && toExpr != null) {
      if (fromExpr.getType().isEmpty()) {
        emitTypeNotResolvedError(fromExpr.getSourceToken(), fromExpr);
      }
      if (toExpr.getType().isEmpty()) {
        emitTypeNotResolvedError(toExpr.getSourceToken(), toExpr);
      }

      var commonType = commonTypeSuperOrTrait.apply(ctx.start, List.of(fromExpr, toExpr));
      rangeExpr.setType(commonType);
    } //Otherwise there would have been unresolved errors earlier.
  }

  private void emitTypeNotResolvedError(final Token lineToken, final ISymbol argument) {
    var msg = "'" + argument.getName() + "':";
    errorListener.semanticError(lineToken, msg,
        ErrorListener.SemanticClassification.TYPE_NOT_RESOLVED);
  }
}

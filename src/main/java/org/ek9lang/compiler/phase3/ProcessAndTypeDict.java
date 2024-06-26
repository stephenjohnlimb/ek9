package org.ek9lang.compiler.phase3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.support.CommonTypeSuperOrTrait;
import org.ek9lang.compiler.support.ParameterisedLocator;
import org.ek9lang.compiler.support.ParameterisedTypeData;
import org.ek9lang.compiler.support.SymbolFactory;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.tokenizer.Ek9Token;
import org.ek9lang.core.AssertValue;

/**
 * Creates/Updates an ek9 Dict (Dictionary/Map) of a specific types if the expressions are typed correctly.
 * This is designed for inferred types.
 */
final class ProcessAndTypeDict extends TypedSymbolAccess implements Consumer<EK9Parser.DictContext> {
  private final ParameterisedLocator parameterisedLocator;
  private final CommonTypeSuperOrTrait commonTypeSuperOrTrait;

  /**
   * Create a new consumer to handle Dict in the form of '{A: X, B: Y, C: Z}'.
   * So A , B and C must be of the same or compatible type (between each other).
   * And X, Y, Z must also be of the same or compatible type (between each other).
   */
  ProcessAndTypeDict(final SymbolAndScopeManagement symbolAndScopeManagement,
                     final SymbolFactory symbolFactory,
                     final ErrorListener errorListener) {

    super(symbolAndScopeManagement, errorListener);

    this.parameterisedLocator =
        new ParameterisedLocator(symbolAndScopeManagement, symbolFactory, errorListener, true);

    this.commonTypeSuperOrTrait = new CommonTypeSuperOrTrait(errorListener);

  }

  @Override
  public void accept(final EK9Parser.DictContext ctx) {

    final var startToken = new Ek9Token(ctx.start);
    //Now we need to get the symbol - but do not expect it to be typed yet.
    //That's the point of this code! See the last part of this method.
    final var dictCallSymbol = symbolAndScopeManagement.getRecordedSymbol(ctx);
    //Access the generic Dict type - this has been pre-located for quicker use.
    final var dictType = symbolAndScopeManagement.getEk9Types().ek9Dictionary();
    final var keyArgumentSymbols = getDictArgumentsAsSymbols(ctx, 0);
    final var valueArgumentSymbols = getDictArgumentsAsSymbols(ctx, 1);
    final var commonKeyType = commonTypeSuperOrTrait.apply(startToken, keyArgumentSymbols);
    final var commonValueType = commonTypeSuperOrTrait.apply(startToken, valueArgumentSymbols);

    if (commonKeyType.isPresent() && commonValueType.isPresent()) {
      final var details =
          new ParameterisedTypeData(startToken, dictType, List.of(commonKeyType.get(), commonValueType.get()));
      dictCallSymbol.setType(parameterisedLocator.resolveOrDefine(details));
    }

  }

  private List<ISymbol> getDictArgumentsAsSymbols(final EK9Parser.DictContext ctx, final int expressionIndex) {

    final List<ISymbol> argumentSymbols = new ArrayList<>();
    for (var initValuePair : ctx.initValuePair()) {
      //But these symbols must have been typed!
      final var exprSymbol = getRecordedAndTypedSymbol(initValuePair.expression(expressionIndex));
      AssertValue.checkNotNull("Compiler error, No initValuePair symbol - missing expression processing?", exprSymbol);
      argumentSymbols.add(exprSymbol);
    }
    return argumentSymbols;
  }
}

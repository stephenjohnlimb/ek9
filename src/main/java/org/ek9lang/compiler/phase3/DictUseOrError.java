package org.ek9lang.compiler.phase3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.support.CommonTypeSuperOrTraitOrError;
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
final class DictUseOrError extends TypedSymbolAccess implements Consumer<EK9Parser.DictContext> {
  private final ParameterisedLocator parameterisedLocator;
  private final CommonTypeSuperOrTraitOrError commonTypeSuperOrTraitOrError;

  /**
   * Create a new consumer to handle Dict in the form of '{A: X, B: Y, C: Z}'.
   * So A , B and C must be of the same or compatible type (between each other).
   * And X, Y, Z must also be of the same or compatible type (between each other).
   */
  DictUseOrError(final SymbolsAndScopes symbolsAndScopes,
                 final SymbolFactory symbolFactory,
                 final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);

    this.parameterisedLocator =
        new ParameterisedLocator(symbolsAndScopes, symbolFactory, errorListener, true);
    this.commonTypeSuperOrTraitOrError = new CommonTypeSuperOrTraitOrError(errorListener);

  }

  @Override
  public void accept(final EK9Parser.DictContext ctx) {

    final var startToken = new Ek9Token(ctx.start);
    //Now we need to get the symbol - but do not expect it to be typed yet.
    //That's the point of this code! See the last part of this method.

    final var commonKeyType = getCommonType(startToken, getDictArgumentsAsSymbols(ctx, 0));
    final var commonValueType = getCommonType(startToken, getDictArgumentsAsSymbols(ctx, 1));

    if (commonKeyType.isPresent() && commonValueType.isPresent()) {
      final var dictCallSymbol = symbolsAndScopes.getRecordedSymbol(ctx);
      final var dictType = symbolsAndScopes.getEk9Types().ek9Dictionary();
      final var details =
          new ParameterisedTypeData(startToken, dictType, List.of(commonKeyType.get(), commonValueType.get()));
      dictCallSymbol.setType(parameterisedLocator.resolveOrDefine(details));
    }

  }

  private Optional<ISymbol> getCommonType(final Ek9Token errorLocation, final List<ISymbol> symbols) {
    return commonTypeSuperOrTraitOrError.apply(errorLocation, symbols);
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

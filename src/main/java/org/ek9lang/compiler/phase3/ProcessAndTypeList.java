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

/**
 * Creates/Updates an ek9 list of a specific type if the expressions are typed correctly.
 */
final class ProcessAndTypeList extends TypedSymbolAccess implements Consumer<EK9Parser.ListContext> {
  private final ParameterisedLocator parameterisedLocator;
  private final CommonTypeSuperOrTrait commonTypeSuperOrTrait;

  /**
   * Create a new consumer to handle Lists in the form of '[X, Y, Z]'.
   */
  ProcessAndTypeList(final SymbolAndScopeManagement symbolAndScopeManagement, final SymbolFactory symbolFactory,
                     final ErrorListener errorListener) {

    super(symbolAndScopeManagement, errorListener);

    this.parameterisedLocator = new ParameterisedLocator(symbolAndScopeManagement, symbolFactory, errorListener, true);
    this.commonTypeSuperOrTrait = new CommonTypeSuperOrTrait(errorListener);

  }

  @Override
  public void accept(final EK9Parser.ListContext ctx) {

    final var startToken = new Ek9Token(ctx.start);
    //Here just like the dictionary - we need the untyped symbol.
    //The task of this code it to provide typing via parameterization.
    final var listCallSymbol = symbolAndScopeManagement.getRecordedSymbol(ctx);
    //Access the generic List type - this has been pre-located for quicker use.
    final var listType = symbolAndScopeManagement.getEk9Types().ek9List();
    final var commonType = commonTypeSuperOrTrait.apply(startToken, getListArgumentsAsSymbols(ctx));
    //If no common type can be found then error will have been emitted
    commonType.ifPresent(type -> {
      final var typeData = new ParameterisedTypeData(startToken, listType, List.of(type));
      final var resolvedNewType = parameterisedLocator.resolveOrDefine(typeData);
      listCallSymbol.setType(resolvedNewType);
    });

  }

  private List<ISymbol> getListArgumentsAsSymbols(final EK9Parser.ListContext ctx) {

    //Maybe move this to a functional stream once all the code is developed.
    final List<ISymbol> argumentSymbols = new ArrayList<>();
    for (var expr : ctx.expression()) {
      final var exprSymbol = getRecordedAndTypedSymbol(expr);
      //Might be null if not valid, but error would have been issued,
      if (exprSymbol != null) {
        argumentSymbols.add(exprSymbol);
      }
    }

    return argumentSymbols;
  }
}

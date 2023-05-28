package org.ek9lang.compiler.main.resolvedefine;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.main.phases.definition.SymbolAndScopeManagement;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.support.CommonTypeSuperOrTrait;
import org.ek9lang.compiler.symbol.support.SymbolFactory;
import org.ek9lang.compiler.symbol.support.search.TemplateTypeSymbolSearch;
import org.ek9lang.core.exception.AssertValue;

/**
 * Creates an ek9 list of a specific type if the expressions are typed correctly.
 */
public class CheckAndTypeList implements Consumer<EK9Parser.ListContext> {

  private final SymbolAndScopeManagement symbolAndScopeManagement;

  private final NewParameterisedType newParameterisedType;
  private final CommonTypeSuperOrTrait commonTypeSuperOrTrait;

  /**
   * Create a new consumer to handle Lists in the form of '[X, Y, Z]'.
   */
  public CheckAndTypeList(final SymbolAndScopeManagement symbolAndScopeManagement,
                          final SymbolFactory symbolFactory,
                          final ErrorListener errorListener) {
    this.symbolAndScopeManagement = symbolAndScopeManagement;

    this.newParameterisedType =
        new NewParameterisedType(symbolAndScopeManagement, symbolFactory, errorListener, true);

    this.commonTypeSuperOrTrait = new CommonTypeSuperOrTrait(errorListener);
  }

  @Override
  public void accept(EK9Parser.ListContext ctx) {

    var listSymbol = symbolAndScopeManagement.getRecordedSymbol(ctx);

    List<ISymbol> argumentSymbols = new ArrayList<>();
    for (var expr : ctx.expression()) {
      var exprSymbol = symbolAndScopeManagement.getRecordedSymbol(expr);
      AssertValue.checkNotNull("Compiler error, No expression symbol - missing expression processing?", exprSymbol);
      argumentSymbols.add(exprSymbol);
    }

    var commonType = commonTypeSuperOrTrait.apply(ctx.start, argumentSymbols);
    commonType.ifPresent(type -> {
      var resolvedType =
          symbolAndScopeManagement.getTopScope().resolve(new TemplateTypeSymbolSearch("org.ek9.lang::List"));
      resolvedType.ifPresent(listType -> {
        var details = new ParameterisedTypeData(ctx.start, listType, List.of(type));
        var resolvedNewType = newParameterisedType.resolveOrDefine(details);
        listSymbol.setType(resolvedNewType);
      });
    });
  }
}

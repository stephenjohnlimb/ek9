package org.ek9lang.compiler.main.resolvedefine;

import java.util.List;
import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.main.phases.definition.SymbolAndScopeManagement;
import org.ek9lang.compiler.symbol.IAggregateSymbol;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.support.search.MethodSearchInScope;
import org.ek9lang.compiler.symbol.support.search.MethodSymbolSearch;

/**
 * Pulls the type from the 'range' into the loop variable, so it is correctly typed.
 * Also checks that if the 'by' literal or identifier is used that the type is compatible with the range.
 */
public class CheckForRange implements Consumer<EK9Parser.ForRangeContext> {

  private final SymbolAndScopeManagement symbolAndScopeManagement;
  private final ResolveIdentifierOrError resolveIdentifierOrError;
  private final ResolveMethodOrError resolveMethodOrError;

  /**
   * Check range expressions and record an expression for the type.
   */
  public CheckForRange(final SymbolAndScopeManagement symbolAndScopeManagement,
                       final ErrorListener errorListener) {
    this.symbolAndScopeManagement = symbolAndScopeManagement;
    this.resolveIdentifierOrError = new ResolveIdentifierOrError(symbolAndScopeManagement, errorListener);
    this.resolveMethodOrError = new ResolveMethodOrError(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(EK9Parser.ForRangeContext ctx) {
    var loopVar = symbolAndScopeManagement.getRecordedSymbol(ctx);
    var rangeExpr = symbolAndScopeManagement.getRecordedSymbol(ctx.range());
    //Now use the type if it has been set
    loopVar.setType(rangeExpr.getType());

    checkForLoopVarComparator(loopVar);

    //Now just check the type of the 'by' part.
    if (ctx.BY() != null) {
      if (ctx.literal() != null) {
        var literal = symbolAndScopeManagement.getRecordedSymbol(ctx.literal());
        checkPlusEqualsOperator(loopVar, literal);
      } else {
        var resolved = resolveIdentifierOrError.apply(ctx.identifier(1));
        if (resolved != null) {
          checkPlusEqualsOperator(loopVar, resolved);
        }
      }
    } else {
      checkOperator(loopVar, "++");
      checkOperator(loopVar, "--");
    }
  }

  private void checkForLoopVarComparator(final ISymbol loopVar) {
    loopVar.getType().ifPresent(loopVarType -> {
      //So the comparator should have one parameter of the same type.
      var search = new MethodSymbolSearch("<=>").setTypeParameters(List.of(loopVarType));
      resolveMethodOrError.apply(loopVar.getSourceToken(),
          new MethodSearchInScope((IAggregateSymbol) loopVarType, search));
    });
  }

  private void checkPlusEqualsOperator(final ISymbol loopVar, final ISymbol incrementBy) {
    loopVar.getType().ifPresent(loopVarType -> incrementBy.getType().ifPresent(incrementByType -> {
      //So the "+=" should have one parameter of the increment by type.
      var search = new MethodSymbolSearch("+=").setTypeParameters(List.of(incrementByType));
      resolveMethodOrError.apply(loopVar.getSourceToken(),
          new MethodSearchInScope((IAggregateSymbol) loopVarType, search));
    }));
  }

  private void checkOperator(final ISymbol loopVar, final String operator) {
    loopVar.getType().ifPresent(loopVarType -> {
      var search = new MethodSymbolSearch(operator);
      resolveMethodOrError.apply(loopVar.getSourceToken(),
          new MethodSearchInScope((IAggregateSymbol) loopVarType, search));
    });
  }
}

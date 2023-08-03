package org.ek9lang.compiler.phase3;

import java.util.List;
import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.search.MethodSearchInScope;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Pulls the type from the 'range' into the loop variable, so it is correctly typed.
 * Also checks that if the 'by' literal or identifier is used that the type is compatible with the range.
 */
final class CheckForRange extends RuleSupport implements Consumer<EK9Parser.ForRangeContext> {

  private final ResolveIdentifierOrError resolveIdentifierOrError;
  private final ResolveMethodOrError resolveMethodOrError;

  /**
   * Check range expressions and record an expression for the type.
   */
  CheckForRange(final SymbolAndScopeManagement symbolAndScopeManagement,
                final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
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

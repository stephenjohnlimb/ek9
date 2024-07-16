package org.ek9lang.compiler.phase3;

import java.util.List;
import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.search.MethodSearchInScope;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Pulls the type from the 'range' into the loop variable, so it is correctly typed.
 * Also checks that if the 'by' literal or identifier is used that the type is compatible with the range.
 */
final class ProcessForRange extends TypedSymbolAccess implements Consumer<EK9Parser.ForRangeContext> {
  private final ProcessIdentifierOrError processIdentifierOrError;
  private final ResolveMethodOrError resolveMethodOrError;

  /**
   * Check range expressions and record an expression for the type.
   */
  ProcessForRange(final SymbolsAndScopes symbolsAndScopes,
                  final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);
    this.processIdentifierOrError = new ProcessIdentifierOrError(symbolsAndScopes, errorListener);
    this.resolveMethodOrError = new ResolveMethodOrError(symbolsAndScopes, errorListener);

  }

  @Override
  public void accept(final EK9Parser.ForRangeContext ctx) {

    //First get the range expression as this will tell us the type for the loop variable
    final var rangeExpr = getRecordedAndTypedSymbol(ctx.range());
    //Note the different call here, we accept that the loop variable will not yet have been 'typed'
    //So now we can set that type on the loop variable.
    final var loopVar = symbolsAndScopes.getRecordedSymbol(ctx);

    if (loopVar != null && rangeExpr != null) {
      checkLoopWithRangeExpression(ctx, loopVar, rangeExpr);

    }
  }

  private void checkLoopWithRangeExpression(final EK9Parser.ForRangeContext ctx,
                                            final ISymbol loopVar,
                                            final ISymbol rangeExpr) {

    loopVar.setType(rangeExpr.getType());
    checkForLoopVarComparator(loopVar);

    //Now just check the type of the 'by' part.
    if (ctx.BY() != null) {
      if (ctx.literal() != null) {
        final var literal = getRecordedAndTypedSymbol(ctx.literal());
        checkPlusEqualsOperator(loopVar, literal);
      } else {
        final var resolved = processIdentifierOrError.apply(ctx.identifier(1));
        checkPlusEqualsOperator(loopVar, resolved);
      }
    } else {
      checkOperator(loopVar, "++");
      checkOperator(loopVar, "--");
    }

  }

  private void checkForLoopVarComparator(final ISymbol loopVar) {

    loopVar.getType().ifPresent(loopVarType -> {
      //So the comparator should have one parameter of the same type.
      final var search = new MethodSymbolSearch("<=>").setTypeParameters(List.of(loopVarType));
      resolveMethodOrError.apply(loopVar.getSourceToken(),
          new MethodSearchInScope((IAggregateSymbol) loopVarType, search));
    });

  }

  private void checkPlusEqualsOperator(final ISymbol loopVar, final ISymbol incrementBy) {
    if (incrementBy != null) {
      loopVar.getType().ifPresent(loopVarType -> incrementBy.getType().ifPresent(incrementByType -> {
        //So the "+=" should have one parameter of the increment by type.
        var search = new MethodSymbolSearch("+=").setTypeParameters(List.of(incrementByType));
        resolveMethodOrError.apply(loopVar.getSourceToken(),
            new MethodSearchInScope((IAggregateSymbol) loopVarType, search));
      }));
    }

  }

  private void checkOperator(final ISymbol loopVar, final String operator) {

    loopVar.getType().ifPresent(loopVarType -> {
      var search = new MethodSymbolSearch(operator);
      resolveMethodOrError.apply(loopVar.getSourceToken(),
          new MethodSearchInScope((IAggregateSymbol) loopVarType, search));
    });

  }
}

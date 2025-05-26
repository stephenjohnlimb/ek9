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
 * Pulls the type from the 'range' into the loop variable, so it is correctly typed or emits an error.
 * Also checks that if the 'by' literal or identifier is used that the type is compatible with the range.
 */
final class ForRangeOrError extends TypedSymbolAccess implements Consumer<EK9Parser.ForRangeContext> {
  private final IdentifierOrError identifierOrError;
  private final ResolveMethodOrError resolveMethodOrError;
  private final ComparatorPresentOrError comparatorPresentOrError;
  private final IncrementPresentOrError incrementPresentOrError;
  private final DecrementPresentOrError decrementPresentOrError;

  /**
   * Check range expressions and record an expression for the type.
   */
  ForRangeOrError(final SymbolsAndScopes symbolsAndScopes,
                  final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);
    this.identifierOrError = new IdentifierOrError(symbolsAndScopes, errorListener);
    this.resolveMethodOrError = new ResolveMethodOrError(symbolsAndScopes, errorListener);
    this.comparatorPresentOrError = new ComparatorPresentOrError(symbolsAndScopes, errorListener);
    this.incrementPresentOrError = new IncrementPresentOrError(symbolsAndScopes, errorListener);
    this.decrementPresentOrError = new DecrementPresentOrError(symbolsAndScopes, errorListener);

  }

  @Override
  public void accept(final EK9Parser.ForRangeContext ctx) {

    //First get the range expression as this will tell us the type for the loop variable
    final var rangeExpr = getRecordedAndTypedSymbol(ctx.range());
    //Note the different call here, we accept that the loop variable will not yet have been 'typed'
    //So now we can set that type on the loop variable.
    final var loopVar = symbolsAndScopes.getRecordedSymbol(ctx);

    if (loopVar != null && rangeExpr != null) {
      loopWithRangeExpressionOrError(ctx, loopVar, rangeExpr);
    }

  }

  private void loopWithRangeExpressionOrError(final EK9Parser.ForRangeContext ctx,
                                              final ISymbol loopVar,
                                              final ISymbol rangeExpr) {

    loopVar.setType(rangeExpr.getType());
    comparatorPresentOrError(loopVar);

    //Now just check the type of the 'by' part.
    if (ctx.BY() != null) {
      if (ctx.literal() != null) {
        final var literal = getRecordedAndTypedSymbol(ctx.literal());
        checkPlusEqualsOperator(loopVar, literal);
      } else {
        final var resolved = identifierOrError.apply(ctx.identifier(1));
        checkPlusEqualsOperator(loopVar, resolved);
      }
    } else {
      incrementAndDecrementPresentOrError(loopVar);
    }

  }

  private void incrementAndDecrementPresentOrError(ISymbol loopVar) {

    loopVar.getType().ifPresent(loopVarType -> {
      incrementPresentOrError.test(loopVar.getSourceToken(), loopVarType);
      decrementPresentOrError.test(loopVar.getSourceToken(), loopVarType);
    });

  }

  private void comparatorPresentOrError(final ISymbol loopVar) {

    loopVar.getType().ifPresent(loopVarType -> comparatorPresentOrError.test(loopVar.getSourceToken(), loopVarType));

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
}

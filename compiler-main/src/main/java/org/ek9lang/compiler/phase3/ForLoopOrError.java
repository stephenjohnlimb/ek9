package org.ek9lang.compiler.phase3;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.MISSING_ITERATE_METHOD;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.support.CommonValues;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * Deals with the for loop for phase 3, which is basically working out the type of the variable being iterated over
 * and ensuring that the expression is something that can be iterated over.
 */
final class ForLoopOrError extends TypedSymbolAccess implements Consumer<EK9Parser.ForLoopContext> {

  private final GetIteratorType getIteratorType;

  ForLoopOrError(final SymbolsAndScopes symbolsAndScopes,
                 final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);
    this.getIteratorType = new GetIteratorType(symbolsAndScopes, errorListener);

  }

  @Override
  public void accept(final EK9Parser.ForLoopContext ctx) {

    //Note the different call here, we accept that the loop variable will not yet have been 'typed'
    //So now we can set that type on the loop variable.
    var loopVar = symbolsAndScopes.getRecordedSymbol(ctx);

    if (loopVar != null) {
      final var expr = getRecordedAndTypedSymbol(ctx.expression());

      if (expr != null
          && expr.getType().isPresent()
          && expr.getType().get() instanceof IAggregateSymbol fromType) {
        updateLoopVariable(loopVar, expr.getSourceToken(), fromType);

        // Squirrel iteration pattern for Phase 10 (IR generation)
        squirrelIterationPattern(ctx, fromType);
      }
    }

  }

  /**
   * Now the loop variable is present but has not yet been typed.
   * The 'fromType' is the type from the expression in the for loop.
   * So now we have to determine the type that the loop variable should be by looking
   * at the fromType, to see if it has an iterator method or 'hasNext' and 'next'.
   */
  private void updateLoopVariable(ISymbol loopVar, IToken errorLocation, final IAggregateSymbol fromType) {

    final var resolvedType = getIteratorType.apply(fromType);
    resolvedType.ifPresentOrElse(
        loopVar::setType,
        () -> emitMissingIteratorMethod(errorLocation, fromType)
    );

  }

  private void emitMissingIteratorMethod(final IToken errorLocation, final ISymbol fromType) {

    final var msg = "iteration over '" + fromType + "' type is not possible:";
    errorListener.semanticError(errorLocation, msg, MISSING_ITERATE_METHOD);

  }

  /**
   * Determine which iteration pattern is used and squirrel it on ForSymbol.
   * This avoids re-doing method resolution work in Phase 10 (IR_GENERATION).
   * <p>
   * Pattern A (ITERATOR_METHOD): collection has iterator() method
   * Pattern B (DIRECT_ITERATION): collection has hasNext()/next() directly
   * </p>
   */
  private void squirrelIterationPattern(final EK9Parser.ForLoopContext ctx,
                                        final IAggregateSymbol collectionType) {

    // Get the ForSymbol for the entire for-statement
    final var forStatementCtx = (EK9Parser.ForStatementExpressionContext) ctx.getParent();
    final var forSymbol = symbolsAndScopes.getRecordedSymbol(forStatementCtx);

    if (forSymbol == null) {
      return; // Defensive - should not happen
    }

    // Check Pattern A: iterator() method returning Iterator<T>
    final var iteratorMethod = collectionType.resolve(new MethodSymbolSearch("iterator"));
    if (iteratorMethod.isPresent()) {
      // Pattern A: Need to call iterator() first
      forSymbol.putSquirrelledData(
          CommonValues.FOR_ITERATION_PATTERN,
          CommonValues.ITERATOR_METHOD.toString()
      );
    } else {
      // Pattern B: Collection IS the iterator (has hasNext/next directly)
      forSymbol.putSquirrelledData(
          CommonValues.FOR_ITERATION_PATTERN,
          CommonValues.DIRECT_ITERATION.toString()
      );
    }

  }

}
package org.ek9lang.compiler.phase3;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.THIS_AND_SUPER_CALLS_ONLY_IN_CONSTRUCTOR;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.THIS_AND_SUPER_MUST_BE_FIRST_IN_CONSTRUCTOR;
import static org.ek9lang.compiler.support.CommonValues.EXPLICIT_SUPER_CALL;
import static org.ek9lang.compiler.support.CommonValues.EXPLICIT_THIS_CALL;
import static org.ek9lang.compiler.support.CommonValues.HAS_EXPLICIT_CONSTRUCTION;

import java.util.List;
import java.util.function.Function;
import org.antlr.v4.runtime.Token;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.MethodAndAggregateData;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.search.MethodSearchInScope;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.support.SymbolTypeExtractor;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.tokenizer.Ek9Token;

/**
 * Just focus on resolving 'this' or 'super' in respect to a 'call'.
 * For example 'this()' or 'super()', but can include parameters.
 * This will also look at the context of where the call is made from as well as the parameter compatibility.
 */
final class ThisOrSuperCallOrError extends TypedSymbolAccess
    implements Function<EK9Parser.CallContext, MethodSymbol> {
  private final SymbolTypeExtractor symbolTypeExtractor = new SymbolTypeExtractor();
  private final ResolveMethodOrError resolveMethodOrError;
  private final SymbolsFromParamExpression symbolsFromParamExpression;

  ThisOrSuperCallOrError(final SymbolsAndScopes symbolsAndScopes,
                         final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);

    this.resolveMethodOrError = new ResolveMethodOrError(symbolsAndScopes, errorListener);
    this.symbolsFromParamExpression = new SymbolsFromParamExpression(symbolsAndScopes, errorListener);

  }

  @Override
  public MethodSymbol apply(final EK9Parser.CallContext ctx) {

    final var methodAndAggregate = symbolsAndScopes.traverseBackUpStackToEnclosingMethod();

    if (methodAndAggregate.isPresent()) {
      validThisOrSuperPositionOrError(ctx, methodAndAggregate.get());
      return resolveMethodSymbolOrError(ctx, methodAndAggregate.get());
    } else {
      if (ctx.primaryReference().THIS() != null) {
        emitInappropriateUseOfThisError(ctx);
      } else {
        emitInappropriateUseOfSuperError(ctx);
      }
    }

    return null;
  }

  /**
   * It's important that the use of this() or super() is the first statement in a constructor.
   * That's what this code does, unfortunately it has to use the grammar structure to work that out.
   * I never really like doing this because its fragile, but in this case I cannot see another way of doing it.
   */
  private void validThisOrSuperPositionOrError(final EK9Parser.CallContext ctx,
                                               final MethodAndAggregateData methodAndAggregateData) {

    if (methodAndAggregateData.methodSymbol().isConstructor()) {
      //I don't really like using the grammar structure like this it s bit fragile.
      final var maybeBlockStatement = ctx.getParent().getParent();

      if (maybeBlockStatement instanceof EK9Parser.BlockStatementContext callBlockStatement) {
        //Then it is at least a statement, nasty - I don't like context stuff like this.
        final var maybeOperationsDetails = ctx.getParent().getParent().getParent().getParent();

        if (maybeOperationsDetails instanceof EK9Parser.OperationDetailsContext details) {
          final var firstBlockStatementContext = details.instructionBlock().blockStatement().getFirst();

          if (firstBlockStatementContext != callBlockStatement) {
            emitThisSuperMustBeFirstStatementError(ctx);
          }

        } else {
          emitThisSuperMustBeFirstStatementError(ctx);
        }
      }

      // Keep the legacy flag for backward compatibility
      methodAndAggregateData.methodSymbol().putSquirrelledData(HAS_EXPLICIT_CONSTRUCTION, "TRUE");

      // Store specific flags to distinguish super() vs this() calls
      if (ctx.primaryReference().SUPER() != null) {
        methodAndAggregateData.methodSymbol().putSquirrelledData(EXPLICIT_SUPER_CALL, "TRUE");
      } else if (ctx.primaryReference().THIS() != null) {
        methodAndAggregateData.methodSymbol().putSquirrelledData(EXPLICIT_THIS_CALL, "TRUE");
      }
    }
  }

  private MethodSymbol resolveMethodSymbolOrError(final EK9Parser.CallContext ctx,
                                                  final MethodAndAggregateData methodAndAggregateData) {

    if (methodAndAggregateData.methodSymbol().isConstructor()) {
      final var aggregate = methodAndAggregateData.aggregateSymbol();
      final var callParams = symbolsFromParamExpression.apply(ctx.paramExpression());

      if (ctx.primaryReference().THIS() != null) {
        return resolveAndRecordOrError(ctx, aggregate, callParams);
      } else {
        final var superAggregate = aggregate.getSuperAggregate();

        if (superAggregate.isPresent()) {
          return resolveAndRecordOrError(ctx, superAggregate.get(), callParams);
        }
      }
    } else {
      emitThisSuperCallsOnlyInConstructorError(ctx);
    }

    return null;
  }

  private MethodSymbol resolveAndRecordOrError(final EK9Parser.CallContext ctx,
                                               final IAggregateSymbol aggregate,
                                               final List<ISymbol> callParams) {

    final var returnMethod = methodOnAggregatePresentOrError(ctx.start, aggregate, aggregate.getName(), callParams);
    if (returnMethod != null) {
      //Just for completeness record against the context, this is the earliest this could have been done.
      recordATypedSymbol(returnMethod, ctx.primaryReference());
    }

    return returnMethod;
  }

  private MethodSymbol methodOnAggregatePresentOrError(final Token token,
                                                       final IAggregateSymbol aggregate,
                                                       final String methodName,
                                                       final List<ISymbol> parameters) {

    final var paramTypes = symbolTypeExtractor.apply(parameters);

    //Maybe earlier types were not defined by the ek9 developer so let's not look at it would be misleading.
    //So if they are not present then there would have been other errors. There is no way can resolve the method.
    if (parameters.size() == paramTypes.size()) {
      final var search = new MethodSymbolSearch(methodName).setTypeParameters(paramTypes);
      return resolveMethodOrError.apply(new Ek9Token(token), new MethodSearchInScope(aggregate, search));
    }

    return null;
  }

  private void emitThisSuperMustBeFirstStatementError(final EK9Parser.CallContext ctx) {

    errorListener.semanticError(ctx.start, "", THIS_AND_SUPER_MUST_BE_FIRST_IN_CONSTRUCTOR);

  }

  private void emitThisSuperCallsOnlyInConstructorError(final EK9Parser.CallContext ctx) {

    errorListener.semanticError(ctx.start, "", THIS_AND_SUPER_CALLS_ONLY_IN_CONSTRUCTOR);

  }

  private void emitInappropriateUseOfThisError(final EK9Parser.CallContext ctx) {

    errorListener.semanticError(ctx.start, "", ErrorListener.SemanticClassification.INAPPROPRIATE_USE_OF_THIS);

  }

  private void emitInappropriateUseOfSuperError(final EK9Parser.CallContext ctx) {

    errorListener.semanticError(ctx.start, "", ErrorListener.SemanticClassification.INAPPROPRIATE_USE_OF_SUPER);

  }
}

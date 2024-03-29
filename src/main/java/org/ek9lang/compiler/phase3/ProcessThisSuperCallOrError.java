package org.ek9lang.compiler.phase3;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.THIS_AND_SUPER_MUST_BE_FIRST_IN_CONSTRUCTOR;

import java.util.List;
import java.util.function.Function;
import org.antlr.v4.runtime.Token;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.MethodAndAggregateData;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
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
final class ProcessThisSuperCallOrError extends TypedSymbolAccess
    implements Function<EK9Parser.CallContext, MethodSymbol> {
  private final SymbolTypeExtractor symbolTypeExtractor = new SymbolTypeExtractor();
  private final ResolveMethodOrError resolveMethodOrError;
  private final SymbolsFromParamExpression symbolsFromParamExpression;

  ProcessThisSuperCallOrError(SymbolAndScopeManagement symbolAndScopeManagement,
                              ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);

    this.resolveMethodOrError = new ResolveMethodOrError(symbolAndScopeManagement, errorListener);
    this.symbolsFromParamExpression = new SymbolsFromParamExpression(symbolAndScopeManagement, errorListener);
  }

  @Override
  public MethodSymbol apply(final EK9Parser.CallContext ctx) {
    var methodAndAggregate = symbolAndScopeManagement.traverseBackUpStackToEnclosingMethod();
    if (methodAndAggregate.isPresent()) {
      checkThisOrSuperStatementPosition(ctx, methodAndAggregate.get());
      return resolveMethodSymbol(ctx, methodAndAggregate.get());
    } else {
      if (ctx.primaryReference().THIS() != null) {
        errorListener.semanticError(ctx.start, "", ErrorListener.SemanticClassification.INAPPROPRIATE_USE_OF_THIS);
      } else {
        errorListener.semanticError(ctx.start, "", ErrorListener.SemanticClassification.INAPPROPRIATE_USE_OF_SUPER);
      }
    }
    return null;
  }

  /**
   * It's important that the use of this() or super() is the first statement in a constructor.
   * That's what this code does, unfortunately it has to use the grammar structure to work that out.
   * I never really like doing this because its fragile, but in this case I cannot see another way of doing it.
   */
  private void checkThisOrSuperStatementPosition(final EK9Parser.CallContext ctx,
                                                 final MethodAndAggregateData methodAndAggregateData) {
    if (methodAndAggregateData.methodSymbol().isConstructor()) {
      //I don't really like using the grammar structure like this it s bit fragile.
      var maybeBlockStatement = ctx.getParent().getParent();
      if (maybeBlockStatement instanceof EK9Parser.BlockStatementContext callBlockStatement) {
        //Then it is at least a statement
        var maybeOperationsDetails = ctx.getParent().getParent().getParent().getParent();
        if (maybeOperationsDetails instanceof EK9Parser.OperationDetailsContext details) {
          var firstBlockStatementContext = details.instructionBlock().blockStatement().get(0);

          if (firstBlockStatementContext != callBlockStatement) {
            errorListener.semanticError(ctx.start, "", THIS_AND_SUPER_MUST_BE_FIRST_IN_CONSTRUCTOR);
          }
        } else {
          errorListener.semanticError(ctx.start, "", THIS_AND_SUPER_MUST_BE_FIRST_IN_CONSTRUCTOR);
        }
      }
    }
  }

  private MethodSymbol resolveMethodSymbol(final EK9Parser.CallContext ctx,
                                           final MethodAndAggregateData methodAndAggregateData) {

    if (methodAndAggregateData.methodSymbol().isConstructor()) {
      var aggregate = methodAndAggregateData.aggregateSymbol();
      var callParams = symbolsFromParamExpression.apply(ctx.paramExpression());
      if (ctx.primaryReference().THIS() != null) {
        return resolveAndRecordIfPossible(ctx, aggregate, callParams);
      } else {
        var superAggregate = aggregate.getSuperAggregate();
        if (superAggregate.isPresent()) {
          return resolveAndRecordIfPossible(ctx, superAggregate.get(), callParams);
        }
      }
    } else {
      errorListener.semanticError(ctx.start, "",
          ErrorListener.SemanticClassification.THIS_AND_SUPER_CALLS_ONLY_IN_CONSTRUCTOR);
    }
    return null;
  }

  private MethodSymbol resolveAndRecordIfPossible(final EK9Parser.CallContext ctx,
                                                  final IAggregateSymbol aggregate,
                                                  final List<ISymbol> callParams) {
    var returnMethod = checkForMethodOnAggregate(ctx.start, aggregate, aggregate.getName(), callParams);
    if (returnMethod != null) {
      //Just for completeness record against the context, this is the earliest this could have been done.
      recordATypedSymbol(returnMethod, ctx.primaryReference());
    }
    return returnMethod;
  }

  private MethodSymbol checkForMethodOnAggregate(final Token token,
                                                 final IAggregateSymbol aggregate,
                                                 final String methodName,
                                                 final List<ISymbol> parameters) {

    var paramTypes = symbolTypeExtractor.apply(parameters);
    //Maybe earlier types were not defined by the ek9 developer so let's not look at it would be misleading.
    //So if they are not present then there would have been other errors. There is no way can resolve the method.
    if (parameters.size() == paramTypes.size()) {
      var search = new MethodSymbolSearch(methodName).setTypeParameters(paramTypes);
      return resolveMethodOrError.apply(new Ek9Token(token), new MethodSearchInScope(aggregate, search));
    }
    return null;
  }
}

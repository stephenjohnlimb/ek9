package org.ek9lang.compiler.phase3;

import java.util.List;
import java.util.function.BiFunction;
import org.antlr.v4.runtime.Token;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.search.MethodSearchInScope;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.search.SymbolSearch;
import org.ek9lang.compiler.support.SymbolTypeExtractor;
import org.ek9lang.compiler.symbols.IScope;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;

/**
 * Used for resolving operation calls on aggregates, which can include properties that are delegates to functions.
 */
final class ResolveOperationCallOrError extends RuleSupport
    implements BiFunction<EK9Parser.OperationCallContext, IScope, ISymbol> {

  private final SymbolTypeExtractor symbolTypeExtractor = new SymbolTypeExtractor();
  private final ResolveMethodOrError resolveMethodOrError;
  private final SymbolsFromParamExpression symbolsFromParamExpression;
  private final CheckValidFunctionDelegateOrError checkValidFunctionDelegateOrError;

  /**
   * Create a new operation resolver.
   */
  ResolveOperationCallOrError(final SymbolAndScopeManagement symbolAndScopeManagement,
                              final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);

    this.symbolsFromParamExpression =
        new SymbolsFromParamExpression(symbolAndScopeManagement, errorListener);
    this.checkValidFunctionDelegateOrError =
        new CheckValidFunctionDelegateOrError(symbolAndScopeManagement, errorListener);
    this.resolveMethodOrError =
        new ResolveMethodOrError(symbolAndScopeManagement, errorListener);
  }

  @Override
  public ISymbol apply(final EK9Parser.OperationCallContext ctx, final IScope scopeToResolveIn) {

    //Get the params and extract the types
    var callParams = symbolsFromParamExpression.apply(ctx.paramExpression());
    var methodOrDelegateName = ctx.operator() != null ? ctx.operator().getText() : ctx.identifier().getText();

    //Firstly just see if we can match a variable - for a delegate match
    var initialCheck = scopeToResolveIn.resolveMember(new SymbolSearch(methodOrDelegateName));
    if (initialCheck.isEmpty()) {
      return resolveAsMethod(ctx, scopeToResolveIn, methodOrDelegateName, callParams);
    }
    return checkValidFunctionDelegateOrError.apply(
        new DelegateFunctionCheckData(ctx.start, initialCheck.get(), callParams));
  }

  private ISymbol resolveAsMethod(final EK9Parser.OperationCallContext ctx,
                                  final IScope scopeToResolveIn,
                                  final String methodName,
                                  final List<ISymbol> callParams) {
    var resolvedMethod = checkForMethodOnAggregate(ctx.start, scopeToResolveIn, methodName, callParams);
    if (resolvedMethod == null) {
      var msg = "'" + methodName + "':";
      errorListener.semanticError(ctx.start, msg, ErrorListener.SemanticClassification.METHOD_NOT_RESOLVED);
      return null;
    }
    resolvedMethod.setReferenced(true);
    symbolAndScopeManagement.recordSymbol(resolvedMethod, ctx);
    return resolvedMethod;
  }

  private MethodSymbol checkForMethodOnAggregate(final Token token,
                                                 final IScope scopeToSearch,
                                                 final String methodName,
                                                 final List<ISymbol> parameters) {

    var paramTypes = symbolTypeExtractor.apply(parameters);
    //maybe earlier types were not defined by the ek9 developer so let's not look at it would be misleading.
    if (parameters.size() == paramTypes.size()) {
      var search = new MethodSymbolSearch(methodName).setTypeParameters(paramTypes);
      return resolveMethodOrError.apply(token, new MethodSearchInScope(scopeToSearch, search));
    }
    return null;
  }
}
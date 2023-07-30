package org.ek9lang.compiler.main.resolvedefine;

import java.util.List;
import java.util.function.BiFunction;
import org.antlr.v4.runtime.Token;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.main.phases.definition.SymbolAndScopeManagement;
import org.ek9lang.compiler.support.RuleSupport;
import org.ek9lang.compiler.symbols.FunctionSymbol;
import org.ek9lang.compiler.symbols.IScope;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.symbols.search.MethodSearchInScope;
import org.ek9lang.compiler.symbols.search.MethodSymbolSearch;
import org.ek9lang.compiler.symbols.search.SymbolSearch;
import org.ek9lang.compiler.symbols.support.SymbolTypeExtractor;
import org.ek9lang.compiler.symbols.support.ToCommaSeparated;

/**
 * Used for resolving operation calls on aggregates.
 */
public class ResolveOperationCallOrError extends RuleSupport
    implements BiFunction<EK9Parser.OperationCallContext, IScope, ISymbol> {

  private final SymbolTypeExtractor symbolTypeExtractor = new SymbolTypeExtractor();

  private final ResolveMethodOrError resolveMethodOrError;


  private final SymbolsFromParamExpression symbolsFromParamExpression;
  private final ResolveFunctionOrError resolveFunctionOrError;

  /**
   * Create a new operation resolver.
   */
  public ResolveOperationCallOrError(final SymbolAndScopeManagement symbolAndScopeManagement,
                                     final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);

    this.symbolsFromParamExpression = new SymbolsFromParamExpression(symbolAndScopeManagement, errorListener);

    this.resolveMethodOrError = new ResolveMethodOrError(symbolAndScopeManagement, errorListener);
    this.resolveFunctionOrError = new ResolveFunctionOrError(symbolAndScopeManagement, errorListener);
  }

  @Override
  public ISymbol apply(final EK9Parser.OperationCallContext ctx, final IScope scopeToResolveIn) {

    //Get the params and extract the types
    var callParams = symbolsFromParamExpression.apply(ctx.paramExpression());
    var methodOrDelegateName = ctx.identifier().getText();

    //Firstly just see if we can match a variable - for a delegate match
    var initialCheck = scopeToResolveIn.resolveMember(new SymbolSearch(methodOrDelegateName));
    if (initialCheck.isEmpty()) {
      return resolveAsMethod(ctx, scopeToResolveIn, methodOrDelegateName, callParams);
    }
    return resolveAsDelegateToFunction(ctx, initialCheck.get(), callParams);
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

  private FunctionSymbol resolveAsDelegateToFunction(EK9Parser.OperationCallContext ctx, ISymbol delegateSymbol,
                                                     List<ISymbol> callParams) {

    if (delegateSymbol.getType().isPresent() && delegateSymbol.getType().get() instanceof FunctionSymbol function) {
      return checkFunctionParameters(ctx.start, function, callParams);
    } else {
      var params = new ToCommaSeparated(true).apply(callParams);
      var msg = "'"
          + delegateSymbol.getFriendlyName()
          + "' used with supplied arguments '"
          + params
          + "':";
      errorListener.semanticError(ctx.start, msg, ErrorListener.SemanticClassification.NOT_A_FUNCTION_DELEGATE);
    }

    return null;
  }

  private FunctionSymbol checkFunctionParameters(Token token, FunctionSymbol function,
                                                 List<ISymbol> parameters) {

    var paramTypes = symbolTypeExtractor.apply(parameters);
    //maybe earlier types were not defined by the ek9 developer so let's not look at it would be misleading.
    if (parameters.size() == paramTypes.size()) {
      return resolveFunctionOrError.apply(new FunctionCheckData(token, function, paramTypes));
    }
    return null;
  }
}
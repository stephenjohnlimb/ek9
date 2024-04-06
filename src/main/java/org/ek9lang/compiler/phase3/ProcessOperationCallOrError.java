package org.ek9lang.compiler.phase3;

import java.util.List;
import java.util.function.BiFunction;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.search.MethodSearchInScope;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.search.SymbolSearch;
import org.ek9lang.compiler.support.MostSpecificScope;
import org.ek9lang.compiler.support.SymbolTypeExtractor;
import org.ek9lang.compiler.symbols.FunctionSymbol;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.IScope;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.symbols.ScopedSymbol;
import org.ek9lang.compiler.tokenizer.Ek9Token;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * Used for resolving operation calls on aggregates, which can include properties that are delegates to functions.
 */
final class ProcessOperationCallOrError extends TypedSymbolAccess
    implements BiFunction<EK9Parser.OperationCallContext, IAggregateSymbol, ScopedSymbol> {
  private final SymbolTypeExtractor symbolTypeExtractor = new SymbolTypeExtractor();
  private final ResolveMethodOrError resolveMethodOrError;
  private final SymbolsFromParamExpression symbolsFromParamExpression;
  private final CheckValidFunctionDelegateOrError checkValidFunctionDelegateOrError;
  private final MostSpecificScope mostSpecificScope;
  private final CheckAccessToSymbol checkAccessToSymbol;
  private final CheckPureContext checkPureContext;

  /**
   * Create a new operation resolver.
   */
  ProcessOperationCallOrError(final SymbolAndScopeManagement symbolAndScopeManagement,
                              final ErrorListener errorListener) {

    super(symbolAndScopeManagement, errorListener);

    this.symbolsFromParamExpression =
        new SymbolsFromParamExpression(symbolAndScopeManagement, errorListener);
    this.checkValidFunctionDelegateOrError =
        new CheckValidFunctionDelegateOrError(symbolAndScopeManagement, errorListener);
    this.resolveMethodOrError =
        new ResolveMethodOrError(symbolAndScopeManagement, errorListener);
    this.mostSpecificScope =
        new MostSpecificScope(symbolAndScopeManagement);
    this.checkAccessToSymbol =
        new CheckAccessToSymbol(symbolAndScopeManagement, errorListener);
    this.checkPureContext =
        new CheckPureContext(symbolAndScopeManagement, errorListener);

  }

  @Override
  public ScopedSymbol apply(final EK9Parser.OperationCallContext ctx, final IAggregateSymbol aggregate) {

    final var startToken = new Ek9Token(ctx.start);
    final var symbol = doProcess(startToken, ctx, aggregate);
    if (symbol != null) {
      checkPureContext.accept(startToken, symbol);
    }

    return symbol;
  }

  private ScopedSymbol doProcess(final IToken startToken,
                                 final EK9Parser.OperationCallContext ctx,
                                 final IAggregateSymbol aggregate) {


    //Get the params and extract the types
    final var callParams = symbolsFromParamExpression.apply(ctx.paramExpression());
    final var methodOrDelegateName = ctx.operator() != null ? ctx.operator().getText() : ctx.identifier().getText();

    //For records the properties are publicly available, but when they are function delegates they 'look' like methods.
    //But the same is true when accessing delegates via 'this' when part of the class itself
    final var resolved = attemptDelegateResolution(startToken, aggregate, methodOrDelegateName);

    if (resolved != null) {
      //Now check if the parameters align. But also need to wrap this into a Call object
      //Also should it be recorded against the context.
      return checkValidFunctionDelegateOrError.apply(new DelegateFunctionCheckData(startToken, resolved, callParams));
    }
    return resolveAsMethod(ctx, aggregate, methodOrDelegateName, callParams);

  }

  private ISymbol attemptDelegateResolution(final IToken startToken,
                                            final IAggregateSymbol aggregate,
                                            final String delegateName) {

    final var accessFromScope = mostSpecificScope.get();
    final var resolutionAttempt = aggregate.resolveMember(new SymbolSearch(delegateName));

    //So only if resolved and is actually a delegate, do we check access and return it.
    if (resolutionAttempt.isPresent()
        && resolutionAttempt.get().getType().isPresent()
        && resolutionAttempt.get().getType().get() instanceof FunctionSymbol) {

      final var resolved = resolutionAttempt.get();
      checkAccessToSymbol.accept(
          new CheckSymbolAccessData(startToken, accessFromScope, aggregate, delegateName, resolved));

      return resolved;
    }

    return null;
  }

  private ScopedSymbol resolveAsMethod(final EK9Parser.OperationCallContext ctx,
                                       final IScope scopeToResolveIn,
                                       final String methodName,
                                       final List<ISymbol> callParams) {

    final var resolvedMethod =
        checkForMethodOnAggregate(new Ek9Token(ctx.start), scopeToResolveIn, methodName, callParams);
    if (resolvedMethod == null) {

      final var msg = "'" + methodName + "':";
      errorListener.semanticError(ctx.start, msg, ErrorListener.SemanticClassification.METHOD_NOT_RESOLVED);
      return null;
    }

    resolvedMethod.setReferenced(true);

    return resolvedMethod;
  }

  private MethodSymbol checkForMethodOnAggregate(final IToken token,
                                                 final IScope scopeToSearch,
                                                 final String methodName,
                                                 final List<ISymbol> parameters) {

    final var paramTypes = symbolTypeExtractor.apply(parameters);
    //maybe earlier types were not defined by the ek9 developer so let's not look, at it would be misleading.
    if (parameters.size() == paramTypes.size()) {
      final var search = new MethodSymbolSearch(methodName).setTypeParameters(paramTypes);
      return resolveMethodOrError.apply(token, new MethodSearchInScope(scopeToSearch, search));
    }

    return null;
  }
}
package org.ek9lang.compiler.phase3;

import java.util.List;
import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.search.MethodSearchInScope;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.support.ParameterisedTypeData;
import org.ek9lang.compiler.support.SymbolFactory;
import org.ek9lang.compiler.support.SymbolTypeExtractor;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.ConstantSymbol;
import org.ek9lang.compiler.symbols.FunctionSymbol;
import org.ek9lang.compiler.symbols.IScope;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.symbols.ScopedSymbol;
import org.ek9lang.compiler.symbols.VariableSymbol;
import org.ek9lang.compiler.tokenizer.Ek9Token;
import org.ek9lang.compiler.tokenizer.IToken;
import org.ek9lang.core.AssertValue;

/**
 * Locate a possible call to an identifierReference, so some sort of call.
 * But this also checks the parameters that are to be passed as arguments, to check that the call is possible.
 */
final class ResolveIdentifierReferenceCallOrError extends TypedSymbolAccess
    implements Function<EK9Parser.CallContext, ScopedSymbol> {
  private final SymbolTypeExtractor symbolTypeExtractor = new SymbolTypeExtractor();
  private final ResolveMethodOrError resolveMethodOrError;
  private final SymbolsFromParamExpression symbolsFromParamExpression;
  private final ParameterisedLocator parameterisedLocator;
  private final CheckValidFunctionDelegateOrError checkValidFunctionDelegateOrError;
  private final ResolveFunctionOrError resolveFunctionOrError;

  private final CheckNotAbstractOrError checkNotAbstractOrError;

  ResolveIdentifierReferenceCallOrError(final SymbolAndScopeManagement symbolAndScopeManagement,
                                        final SymbolFactory symbolFactory,
                                        final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);

    this.resolveMethodOrError =
        new ResolveMethodOrError(symbolAndScopeManagement, errorListener);
    this.symbolsFromParamExpression =
        new SymbolsFromParamExpression(symbolAndScopeManagement, errorListener);
    this.parameterisedLocator =
        new ParameterisedLocator(symbolAndScopeManagement, symbolFactory, errorListener, true);
    this.checkValidFunctionDelegateOrError =
        new CheckValidFunctionDelegateOrError(symbolAndScopeManagement, errorListener);
    this.resolveFunctionOrError =
        new ResolveFunctionOrError(symbolAndScopeManagement, errorListener);
    this.checkNotAbstractOrError =
        new CheckNotAbstractOrError(symbolAndScopeManagement, errorListener);
  }

  @Override
  public ScopedSymbol apply(final EK9Parser.CallContext ctx) {
    var callParams = symbolsFromParamExpression.apply(ctx.paramExpression());
    //This might not be resolved yet for methods for example.
    var callIdentifier = symbolAndScopeManagement.getRecordedSymbol(ctx.identifierReference());
    var startToken = new Ek9Token(ctx.start);
    return switch (callIdentifier) {
      case AggregateSymbol aggregate -> checkAggregate(startToken, aggregate, callIdentifier, callParams);
      case FunctionSymbol function -> checkFunction(startToken, function, callIdentifier, callParams);
      case MethodSymbol method ->
          checkForMethodOnAggregate(startToken, method.getParentScope(), callIdentifier.getName(), callParams);
      case VariableSymbol variable ->
          checkValidFunctionDelegateOrError.apply(new DelegateFunctionCheckData(startToken, variable, callParams));
      case ConstantSymbol constantSymbol -> {
        errorListener.semanticError(ctx.start, "'" + constantSymbol + "' is a constant:",
            ErrorListener.SemanticClassification.TYPE_MUST_BE_FUNCTION);
        yield null;
      }
      case null -> {
        errorListener.semanticError(ctx.start, "", ErrorListener.SemanticClassification.NOT_RESOLVED);
        yield null;
      }
      default -> {
        AssertValue.fail(
            "Compiler error: Not expecting " + ctx.getText() + " as [" + callIdentifier.getFriendlyName() + "]");
        yield null;
      }
    };
  }

  private ScopedSymbol checkAggregate(final IToken token,
                                      final AggregateSymbol aggregate,
                                      final ISymbol callIdentifier,
                                      final List<ISymbol> callArguments) {

    checkNotAbstractOrError.accept(token, callIdentifier);

    if (aggregate.isGenericInNature()) {
      //So if it is generic but no parameters, just return the generic type.
      //let any assignments of checks with inference check the type compatibility or alter the type as appropriate.

      if (callArguments.isEmpty()) {
        //This enables: 'aList as List of Float: List()'
        //But requires that assignment alters the 'call' type and invocation.
        return aggregate;
      } else {
        return checkGenericConstructionOrInvocation(token, callIdentifier, callArguments);
      }
    }
    //It's just a simple call to a constructor, but what if it's an abstract type.
    return checkForMethodOnAggregate(token, aggregate, callIdentifier.getName(), callArguments);
  }

  private ScopedSymbol checkFunction(final IToken token,
                                     final FunctionSymbol function,
                                     final ISymbol callIdentifier,
                                     final List<ISymbol> callArguments) {

    checkNotAbstractOrError.accept(token, callIdentifier);

    if (function.isGenericInNature()) {
      if (callArguments.isEmpty()) {
        //This enables: 'checkFunction1 as GenericFunction1 of Integer: GenericFunction1()'
        //But requires that assignment alters the 'call' type and invocation.
        return function;
      } else {
        return checkGenericConstructionOrInvocation(token, callIdentifier, callArguments);
      }
    }
    return checkFunctionParameters(token, function, callArguments);
  }

  private ScopedSymbol checkGenericConstructionOrInvocation(final IToken token,
                                                            final ISymbol genericSymbol,
                                                            final List<ISymbol> parameters) {
    var genericTypeArguments = symbolTypeExtractor.apply(parameters);
    //maybe earlier types were not defined by the ek9 developer so let's not look at it would be misleading.
    if (parameters.size() == genericTypeArguments.size()) {
      var theParameterisedType =
          parameterisedLocator.apply(new ParameterisedTypeData(token, genericSymbol, genericTypeArguments));
      if (theParameterisedType.isPresent()) {
        return (ScopedSymbol) theParameterisedType.get();
      }
    }
    return null;
  }

  private ScopedSymbol checkFunctionParameters(final IToken token,
                                               final FunctionSymbol function,
                                               final List<ISymbol> parameters) {
    var paramTypes = symbolTypeExtractor.apply(parameters);
    //maybe earlier types were not defined by the ek9 developer so let's not look at it would be misleading.
    if (parameters.size() == paramTypes.size()) {
      return resolveFunctionOrError.apply(new FunctionCheckData(token, function, paramTypes));
    }
    return null;
  }

  private MethodSymbol checkForMethodOnAggregate(final IToken token,
                                                 final IScope aggregate,
                                                 final String methodName,
                                                 final List<ISymbol> parameters) {

    var paramTypes = symbolTypeExtractor.apply(parameters);
    //Maybe earlier types were not defined by the ek9 developer so let's not look at it would be misleading.
    //So if they are not present then there would have been other errors. There is no way can resolve the method.
    if (parameters.size() == paramTypes.size()) {
      var search = new MethodSymbolSearch(methodName).setTypeParameters(paramTypes);
      return resolveMethodOrError.apply(token, new MethodSearchInScope(aggregate, search));
    }
    return null;
  }
}

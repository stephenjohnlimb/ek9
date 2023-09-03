package org.ek9lang.compiler.phase3;

import java.util.List;
import java.util.function.Function;
import org.antlr.v4.runtime.Token;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.search.MethodSearchInScope;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.support.ParameterisedTypeData;
import org.ek9lang.compiler.support.SymbolFactory;
import org.ek9lang.compiler.support.SymbolTypeExtractor;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.FunctionSymbol;
import org.ek9lang.compiler.symbols.IScope;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.symbols.ScopedSymbol;
import org.ek9lang.compiler.symbols.VariableSymbol;

/**
 * Locate a possible call to an identifierReference, so some sort of call.
 * But this also checks the parameters that are to be passed as arguments, to check that the call is possible.
 */
final class ResolveIdentifierReferenceCallOrError extends RuleSupport
    implements Function<EK9Parser.CallContext, ScopedSymbol> {
  private final SymbolTypeExtractor symbolTypeExtractor = new SymbolTypeExtractor();
  private final ResolveMethodOrError resolveMethodOrError;
  private final SymbolsFromParamExpression symbolsFromParamExpression;
  private final SymbolFromContextOrError symbolFromContextOrError;
  private final ParameterisedLocator parameterisedLocator;
  private final CheckValidFunctionDelegateOrError checkValidFunctionDelegateOrError;
  private final ResolveFunctionOrError resolveFunctionOrError;

  ResolveIdentifierReferenceCallOrError(final SymbolAndScopeManagement symbolAndScopeManagement,
                                        final SymbolFactory symbolFactory,
                                        final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);

    this.resolveMethodOrError =
        new ResolveMethodOrError(symbolAndScopeManagement, errorListener);
    this.symbolsFromParamExpression =
        new SymbolsFromParamExpression(symbolAndScopeManagement, errorListener);
    this.symbolFromContextOrError =
        new SymbolFromContextOrError(symbolAndScopeManagement, errorListener);
    this.parameterisedLocator =
        new ParameterisedLocator(symbolAndScopeManagement, symbolFactory, errorListener, true);
    this.checkValidFunctionDelegateOrError =
        new CheckValidFunctionDelegateOrError(symbolAndScopeManagement, errorListener);
    this.resolveFunctionOrError =
        new ResolveFunctionOrError(symbolAndScopeManagement, errorListener);
  }

  @Override
  public ScopedSymbol apply(final EK9Parser.CallContext ctx) {
    //TODO refactor - too complex
    var callParams = symbolsFromParamExpression.apply(ctx.paramExpression());
    //function/method/constructor/delegate with optional params
    var callIdentifier = symbolFromContextOrError.apply(ctx.identifierReference());

    //TODO more but let's see what happens just for 'class' and 'function' for now.
    if (callIdentifier instanceof AggregateSymbol aggregate) {
      if (aggregate.isGenericInNature()) {
        //So if it is generic but no parameters, just return the generic type.
        //let any assignments of checks with inference check the type compatibility or alter the type as appropriate.
        if (callParams.isEmpty()) {
          return aggregate;
        } else {
          var genericTypeArguments = this.symbolTypeExtractor.apply(callParams);
          var details = new ParameterisedTypeData(ctx.start, callIdentifier, genericTypeArguments);
          var theParameterisedType = parameterisedLocator.apply(details);
          System.out.println("Looks like this aggregate needs to be parameterised " + theParameterisedType);
          if (theParameterisedType.isPresent()) {
            return (ScopedSymbol) theParameterisedType.get();
          }
        }
      } else {
        return checkForMethodOnAggregate(ctx.start, aggregate, callIdentifier.getName(), callParams);
      }
    } else if (callIdentifier instanceof FunctionSymbol function) {
      if (function.isGenericInNature()) {
        //TODO function call to generic
      } else {
        return checkFunctionParameters(ctx.start, function, callParams);
      }
    } else if (callIdentifier instanceof MethodSymbol method) {
      return checkForMethodOnAggregate(ctx.start, method.getParentScope(), callIdentifier.getName(), callParams);
    } else if (callIdentifier instanceof VariableSymbol variable) {
      return checkValidFunctionDelegateOrError.apply(new DelegateFunctionCheckData(ctx.start, variable, callParams));
      //While this does not seem to make sense, it is possible to have a variable that is a delegate to a function
    } else {
      //Could just be a 'call' to something that is not resolved!
      //TODO! Consider another error - here - but we may already have one.
      //System.out.println("Not sure what it is [" + callIdentifier + "] [" + ctx.getText() + "] ");
    }

    //So we now have the combinations of what is needed, now it just depends on 'what it is'
    return null;
  }

  private ScopedSymbol checkFunctionParameters(Token token, FunctionSymbol function,
                                               List<ISymbol> parameters) {
    var paramTypes = symbolTypeExtractor.apply(parameters);
    //maybe earlier types were not defined by the ek9 developer so let's not look at it would be misleading.
    if (parameters.size() == paramTypes.size()) {
      return resolveFunctionOrError.apply(new FunctionCheckData(token, function, paramTypes));
    }
    return null;
  }


  private MethodSymbol checkForMethodOnAggregate(final Token token,
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

package org.ek9lang.compiler.phase3;

import java.util.List;
import java.util.function.Consumer;
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
import org.ek9lang.compiler.symbols.CallSymbol;
import org.ek9lang.compiler.symbols.FunctionSymbol;
import org.ek9lang.compiler.symbols.IScope;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.symbols.ScopedSymbol;
import org.ek9lang.compiler.symbols.VariableSymbol;
import org.ek9lang.core.AssertValue;

/**
 * TODO lots of tidying up.
 */
final class CheckValidCall extends RuleSupport implements Consumer<EK9Parser.CallContext> {

  private final SymbolTypeExtractor symbolTypeExtractor = new SymbolTypeExtractor();

  private final ResolveMethodOrError resolveMethodOrError;

  private final ResolveFunctionOrError resolveFunctionOrError;

  private final ResolveThisSuperOrError resolveThisSuperOrError;

  private final ParameterisedLocator parameterisedLocator;

  private final SymbolsFromParamExpression symbolsFromParamExpression;

  private final SymbolFromContextOrError symbolFromContextOrError;
  private final CheckValidFunctionDelegateOrError checkValidFunctionDelegateOrError;

  /**
   * Lookup a pre-recorded 'call', now resolve what it is supposed to call and set it's type.
   */
  CheckValidCall(final SymbolAndScopeManagement symbolAndScopeManagement,
                 final SymbolFactory symbolFactory,
                 final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
    this.resolveMethodOrError = 
        new ResolveMethodOrError(symbolAndScopeManagement, errorListener);
    this.resolveFunctionOrError =
        new ResolveFunctionOrError(symbolAndScopeManagement, errorListener);
    this.resolveThisSuperOrError =
        new ResolveThisSuperOrError(symbolAndScopeManagement, errorListener);
    this.parameterisedLocator = 
        new ParameterisedLocator(symbolAndScopeManagement, symbolFactory, errorListener, true);
    this.symbolsFromParamExpression = 
        new SymbolsFromParamExpression(symbolAndScopeManagement, errorListener);
    this.symbolFromContextOrError =
        new SymbolFromContextOrError(symbolAndScopeManagement, errorListener);
    this.checkValidFunctionDelegateOrError =
        new CheckValidFunctionDelegateOrError(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(final EK9Parser.CallContext ctx) {
    var existingCallSymbol = symbolAndScopeManagement.getRecordedSymbol(ctx);
    if (existingCallSymbol instanceof CallSymbol callSymbol) {
      resolveToBeCalled(callSymbol, ctx);
    } else {
      AssertValue.fail("Compiler error: ValidateCall expecting a CallSymbol to have been recorded");
    }
  }

  /**
   * This is where it is important to resolve the something to be called.
   * This could be a function, method, Constructor, variable that has a type of (TEMPLATE)_FUNCTION (i.e. a delegate).
   * So we can use any existing context below where those are now resolved.
   * But we also need to apply - the appropriate parameters where appropriate to do the resolution.
   * <pre>
   *     identifierReference paramExpression - function/method/constructor/delegate with optional params
   *     | parameterisedType - the parameterization of a generic type, ie List of String for example
   *     | primaryReference paramExpression - this or super
   *     | dynamicFunctionDeclaration
   *     | call paramExpression - a bit weird but supports someHigherFunction()("Call what was returned")
   * </pre>
   */
  private void resolveToBeCalled(CallSymbol callSymbol, final EK9Parser.CallContext ctx) {
    ScopedSymbol symbol = null;
    boolean formOfDeclaration = false;
    if (ctx.identifierReference() != null) {
      symbol = resolveByIdentifierReference(ctx);
    } else if (ctx.parameterisedType() != null) {
      symbol = resolveByParameterisedType(ctx);
    } else if (ctx.primaryReference() != null) {
      symbol = resolveByPrimaryReference(ctx);
    } else if (ctx.dynamicFunctionDeclaration() != null) {
      symbol = resolveByDynamicFunctionDeclaration(ctx);
      formOfDeclaration = true;
    } else if (ctx.call() != null) {
      symbol = resolveByCall(ctx);
    } else {
      AssertValue.fail("Expecting finite set of operations on call " + ctx.start.getLine());
    }
    //If the ek9 source code was not correct, it is possible that we cannot resolve what the call is for
    //So we leave the call symbol as is - with an unresolved 'thing' it should have been calling.
    if (symbol != null) {
      callSymbol.setFormOfDeclarationCall(formOfDeclaration);
      callSymbol.setResolvedSymbolToCall(symbol);
    }
  }

  private ScopedSymbol resolveByIdentifierReference(EK9Parser.CallContext ctx) {
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

  /**
   * Resolve the instantiation of a generic types that has been parameterized.
   */
  private ScopedSymbol resolveByParameterisedType(EK9Parser.CallContext ctx) {
    return (ScopedSymbol) symbolFromContextOrError.apply(ctx.parameterisedType());
  }

  /**
   * Resolve the dynamic function from the context.
   */
  private ScopedSymbol resolveByDynamicFunctionDeclaration(EK9Parser.CallContext ctx) {
    return (ScopedSymbol) symbolFromContextOrError.apply(ctx.dynamicFunctionDeclaration());
  }

  /**
   * This can only be 'this' or 'super'.
   * So we're looking for a Constructor on the type or a constructor on the super type.
   */
  private ScopedSymbol resolveByPrimaryReference(EK9Parser.CallContext ctx) {
    return resolveThisSuperOrError.apply(ctx);
  }

  /**
   * Now we'd be getting something back from another call.
   * We need to check that it is possible to make that call with the parameters provided.
   */
  private ScopedSymbol resolveByCall(EK9Parser.CallContext ctx) {
    //TODO sort out
    var callParams = symbolsFromParamExpression.apply(ctx.paramExpression());
    var callIdentifier = symbolFromContextOrError.apply(ctx.call());
    //Check if the actual call was resolved, then we can get it and see if it will accept these callParameters.
    if (callIdentifier != null) {
      System.out.println("Resolved callIdentifier as [" + callIdentifier.getFriendlyName() + "]");
    } else {
      System.out.println("callIdentifier is null for [" + ctx.getText() + "]");
    }

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

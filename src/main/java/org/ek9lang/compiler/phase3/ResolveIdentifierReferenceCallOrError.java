package org.ek9lang.compiler.phase3;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.TYPE_MUST_BE_FUNCTION;
import static org.ek9lang.compiler.support.CommonValues.ACCESSED;

import java.util.List;
import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.search.MethodSearchInScope;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.search.PossibleMatchingMethods;
import org.ek9lang.compiler.support.MostSpecificScope;
import org.ek9lang.compiler.support.ParameterisedLocator;
import org.ek9lang.compiler.support.ParameterisedTypeData;
import org.ek9lang.compiler.support.SymbolFactory;
import org.ek9lang.compiler.support.SymbolTypeExtractor;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.ConstantSymbol;
import org.ek9lang.compiler.symbols.FunctionSymbol;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
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
  private final PossibleMatchingMethods possibleMatchingMethods = new PossibleMatchingMethods();
  private final MostSpecificScope mostSpecificScope;
  private final ResolveMethodOrError resolveMethodOrError;
  private final SymbolsFromParamExpression symbolsFromParamExpression;
  private final ParameterisedLocator parameterisedLocator;
  private final FunctionDelegateOrError functionDelegateOrError;
  private final ResolveFunctionOrError resolveFunctionOrError;

  private final NotAbstractOrError notAbstractOrError;

  ResolveIdentifierReferenceCallOrError(final SymbolsAndScopes symbolsAndScopes,
                                        final SymbolFactory symbolFactory,
                                        final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);
    this.mostSpecificScope =
        new MostSpecificScope(symbolsAndScopes);
    this.resolveMethodOrError =
        new ResolveMethodOrError(symbolsAndScopes, errorListener);
    this.symbolsFromParamExpression =
        new SymbolsFromParamExpression(symbolsAndScopes, errorListener);
    this.parameterisedLocator =
        new ParameterisedLocator(symbolsAndScopes, symbolFactory, errorListener, true);
    this.functionDelegateOrError =
        new FunctionDelegateOrError(symbolsAndScopes, errorListener);
    this.resolveFunctionOrError =
        new ResolveFunctionOrError(symbolsAndScopes, errorListener);
    this.notAbstractOrError =
        new NotAbstractOrError(symbolsAndScopes, errorListener);
  }

  @Override
  public ScopedSymbol apply(final EK9Parser.CallContext ctx) {

    final var callParams = symbolsFromParamExpression.apply(ctx.paramExpression());
    final var callIdentifier = symbolsAndScopes.getRecordedSymbol(ctx.identifierReference());
    final var startToken = new Ek9Token(ctx.start);

    return switch (callIdentifier) {
      case AggregateSymbol aggregate -> checkAggregate(startToken, aggregate, callIdentifier, callParams);
      case FunctionSymbol function -> checkFunction(startToken, function, callIdentifier, callParams);
      case MethodSymbol method ->
          checkForMethodOnAggregate(startToken, (IAggregateSymbol) method.getParentScope(), callIdentifier.getName(),
              callParams);
      case VariableSymbol variable ->
          checkForDelegateOrSearchForMethod(startToken, ctx.identifierReference(), variable, callParams);
      case ConstantSymbol constantSymbol -> emitGotAConstantNotAFunctionError(ctx, constantSymbol);
      case null -> emitFunctionNotResolved(ctx);
      default -> {
        AssertValue.fail(
            "Compiler error: Not expecting " + ctx.getText() + " as [" + callIdentifier.getFriendlyName() + "]");
        yield null;
      }
    };
  }

  private ScopedSymbol checkForDelegateOrSearchForMethod(final Ek9Token startToken,
                                                         final EK9Parser.IdentifierReferenceContext ctx,
                                                         final VariableSymbol variable,
                                                         final List<ISymbol> callParams) {

    //Do a quick check (no errors) to see if there is a method that may match
    final var scope = mostSpecificScope.get();
    final var searchDetails = new MethodSearchInScope(scope, new MethodSymbolSearch(variable.getName()));
    final var possibleMethods = possibleMatchingMethods.apply(searchDetails);

    //If the nearest identifier during normal resolution is a variable we may check if it is a suitable delegate
    if (possibleMethods.isEmpty()
        || variable.getType().isPresent() && variable.getType().get() instanceof FunctionSymbol) {
      return functionDelegateOrError.apply(new DelegateFunctionData(startToken, variable, callParams));
    }

    //Or just try and resolve a method with that name.
    final var resolvedMethod =
        checkForMethodOnAggregate(startToken, (IAggregateSymbol) scope, variable.getName(), callParams);

    if (resolvedMethod != null) {
      //We now update the recorded system for this identifier reference.
      recordATypedSymbol(resolvedMethod, ctx);
    }

    return resolvedMethod;
  }

  private ScopedSymbol checkAggregate(final IToken token,
                                      final AggregateSymbol aggregate,
                                      final ISymbol callIdentifier,
                                      final List<ISymbol> callArguments) {

    notAbstractOrError.accept(token, callIdentifier);

    if (aggregate.isGenericInNature()) {
      //So if it is generic but no parameters, just return the generic type.
      //let any assignments of checks with inference check the type compatibility or alter the type as appropriate.

      if (callArguments.isEmpty()) {
        emitMustBeParameterized(token, callIdentifier);
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

    notAbstractOrError.accept(token, callIdentifier);

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
    final var genericTypeArguments = symbolTypeExtractor.apply(parameters);
    //maybe earlier types were not defined by the ek9 developer so let's not look at it would be misleading.
    if (parameters.size() == genericTypeArguments.size()) {
      final var theParameterisedType =
          parameterisedLocator.apply(new ParameterisedTypeData(token, genericSymbol, genericTypeArguments));
      if (theParameterisedType.isPresent()) {
        //Now I think we should not return the 'type', but the appropriate constructor method.
        if (theParameterisedType.get() instanceof IAggregateSymbol asParameterisedAggregate) {
          final var resolvedConstructor = asParameterisedAggregate.resolveInThisScopeOnly(
              new MethodSymbolSearch(asParameterisedAggregate.getName()).setTypeParameters(parameters));
          if (resolvedConstructor.isPresent()) {
            return (ScopedSymbol) resolvedConstructor.get();
          }
        } else {
          //It is a generic function.
          return (ScopedSymbol) theParameterisedType.get();
        }

      }
    }

    return null;
  }

  private ScopedSymbol checkFunctionParameters(final IToken token,
                                               final FunctionSymbol function,
                                               final List<ISymbol> parameters) {

    final var paramTypes = symbolTypeExtractor.apply(parameters);
    //maybe earlier types were not defined by the ek9 developer so let's not look at it would be misleading.
    if (parameters.size() == paramTypes.size()) {
      return resolveFunctionOrError.apply(new FunctionData(token, function, paramTypes));
    }

    return null;
  }

  private MethodSymbol checkForMethodOnAggregate(final IToken token,
                                                 final IAggregateSymbol aggregate,
                                                 final String methodName,
                                                 final List<ISymbol> parameters) {

    final var paramTypes = symbolTypeExtractor.apply(parameters);

    //Maybe earlier types were not defined by the ek9 developer so let's not look at it would be misleading.
    //So if they are not present then there would have been other errors. There is no way can resolve the method.

    if (parameters.size() == paramTypes.size()) {
      final var search = new MethodSymbolSearch(methodName).setTypeParameters(paramTypes);
      final var resolved = resolveMethodOrError.apply(token, new MethodSearchInScope(aggregate, search));

      if (resolved != null && aggregate.isConceptualTypeParameter()) {
        resolved.putSquirrelledData(ACCESSED, "TRUE");
      }

      return resolved;
    }

    return null;
  }

  private ScopedSymbol emitGotAConstantNotAFunctionError(final EK9Parser.CallContext ctx,
                                                         final ConstantSymbol constantSymbol) {

    errorListener.semanticError(ctx.start, "'" + constantSymbol + "' is a constant:", TYPE_MUST_BE_FUNCTION);
    return null;
  }

  private ScopedSymbol emitFunctionNotResolved(final EK9Parser.CallContext ctx) {

    errorListener.semanticError(ctx.start, "", ErrorListener.SemanticClassification.NOT_RESOLVED);
    return null;
  }

  private void emitMustBeParameterized(final IToken errorLocation,
                                       final ISymbol property) {

    final var msg = "wrt '" + property.getName() + "' "
        + "Generic/Template construction" + ":";

    errorListener.semanticError(errorLocation, msg,
        ErrorListener.SemanticClassification.GENERIC_TYPE_OR_FUNCTION_PARAMETERS_NEEDED);

  }

}
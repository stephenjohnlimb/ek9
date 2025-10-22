package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.support.AccessGenericInGeneric;
import org.ek9lang.compiler.support.ParameterizationComplete;
import org.ek9lang.compiler.support.SymbolFactory;
import org.ek9lang.compiler.symbols.CallSymbol;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.ScopedSymbol;
import org.ek9lang.compiler.tokenizer.Ek9Token;
import org.ek9lang.core.AssertValue;

/**
 * Designed to deal with this following part of the EK9 grammar.
 * This is one of the trickiest bits, because we want a simple syntax for the EK9 developer.
 * But it means that a call like 'something(...)' could mean quite a few things.
 * <pre>
 *     identifierReference paramExpression - function/method/constructor/delegate with optional params
 *     | parameterisedType - the parameterization of a generic type, ie List of String for example
 *     | primaryReference paramExpression - this or super
 *     | dynamicFunctionDeclaration
 *     | call paramExpression - a bit weird but supports someHigherFunction()("Call what was returned")
 * </pre>
 */
final class CallOrError extends TypedSymbolAccess implements Consumer<EK9Parser.CallContext> {
  private final ParameterizationComplete parameterizationComplete = new ParameterizationComplete();
  private final ThisOrSuperCallOrError thisOrSuperCallOrError;
  private final ResolveIdentifierReferenceCallOrError resolveIdentifierReferenceCallOrError;
  private final SymbolsFromParamExpression symbolsFromParamExpression;
  private final SymbolFromContextOrError symbolFromContextOrError;
  private final FunctionDelegateOrError functionDelegateOrError;
  private final AccessGenericInGeneric accessGenericInGeneric;
  private final PureProcessingInPureContextOrError pureProcessingInPureContextOrError;

  private final ValidNamedArgumentsOrError validNamedArgumentsOrError;

  /**
   * Lookup a pre-recorded 'call', now resolve what it is supposed to call and set its type.
   */
  CallOrError(final SymbolsAndScopes symbolsAndScopes,
              final SymbolFactory symbolFactory,
              final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);
    this.thisOrSuperCallOrError =
        new ThisOrSuperCallOrError(symbolsAndScopes, errorListener);
    this.resolveIdentifierReferenceCallOrError =
        new ResolveIdentifierReferenceCallOrError(symbolsAndScopes, symbolFactory, errorListener);
    this.symbolsFromParamExpression =
        new SymbolsFromParamExpression(symbolsAndScopes, errorListener);
    this.symbolFromContextOrError =
        new SymbolFromContextOrError(symbolsAndScopes, errorListener);
    this.functionDelegateOrError =
        new FunctionDelegateOrError(symbolsAndScopes, errorListener);
    this.accessGenericInGeneric =
        new AccessGenericInGeneric(symbolsAndScopes);
    this.pureProcessingInPureContextOrError =
        new PureProcessingInPureContextOrError(symbolsAndScopes, errorListener);
    this.validNamedArgumentsOrError =
        new ValidNamedArgumentsOrError(symbolsAndScopes, errorListener);
  }

  @Override
  public void accept(final EK9Parser.CallContext ctx) {

    //Note that we make an untyped check to get the call - because until we resolve
    //the thing we're going to call we won't know that type this existingCallSymbol is.

    final var existingCallSymbol = symbolsAndScopes.getRecordedSymbol(ctx);

    if (existingCallSymbol instanceof CallSymbol callSymbol) {
      resolveToBeCalledOrError(callSymbol, ctx);
    } else {
      //So this is a full hard stop, compiler error in itself.
      AssertValue.fail("Compiler error: ValidateCall expecting a CallSymbol to have been recorded");
    }
  }

  /**
   * This is where it is important to resolve the something to be called.
   * This could be a function, method, Constructor, variable that has a type of (TEMPLATE)_FUNCTION (i.e. a delegate).
   * So we can use any existing context below where those are now resolved.
   * But we also need to apply - the appropriate parameters where appropriate to do the resolution.
   * <br/>
   * There are some tricky bits around detecting dynamicFunction calls, and Generics initialisations.
   * This is especially true around generics within generics.
   */
  private void resolveToBeCalledOrError(CallSymbol callSymbol, final EK9Parser.CallContext ctx) {

    if (ctx.identifierReference() != null) {
      resolveByIdentifierReferenceOrError(callSymbol, ctx);
    } else if (ctx.parameterisedType() != null) {
      resolveByParameterisedTypeOrError(callSymbol, ctx);
    } else if (ctx.primaryReference() != null) {
      resolveByPrimaryReferenceOrError(callSymbol, ctx);
    } else if (ctx.dynamicFunctionDeclaration() != null) {
      resolveByDynamicFunctionOrError(callSymbol, ctx);
    } else if (ctx.call() != null) {
      resolveByCallOrError(callSymbol, ctx);
    } else {
      AssertValue.fail("Expecting finite set of operations on call " + ctx.start.getLine());
    }

  }

  /**
   * This can be quite a few different 'types' of identifierReference.
   * So the processing in here is quite detailed with quite a few combinations and paths.
   */
  private void resolveByIdentifierReferenceOrError(final CallSymbol callSymbol, final EK9Parser.CallContext ctx) {

    final var symbol = resolveIdentifierReferenceCallOrError.apply(ctx);


    //Now this is where the developer has written 'l as List of String : List()'
    //Or fun as SomeFunction of Integer: SomeFunction().
    if (symbol != null) {
      validNamedArgumentsOrError.accept(ctx.paramExpression(), symbol.getSymbolsForThisScope());

      //We must also cater for generics within generics.
      //Just checking isGenericInNature is not enough when used within a generic type.
      final var genericInGenericData = accessGenericInGeneric.apply(symbol);

      if (genericInGenericData.isPresent()) {
        callSymbol.setFormOfDeclarationCall(!parameterizationComplete.test(genericInGenericData.get()));
      } else {
        callSymbol.setFormOfDeclarationCall(symbol.isGenericInNature());
      }

      callSymbol.setResolvedSymbolToCall(symbol);
      pureProcessingInPureContextOrError.accept(callSymbol.getSourceToken(), callSymbol);
    }
  }

  /**
   * Resolve the instantiation of a generic type that has been parameterized.
   * This is simple as it will have already been recorded against that context.
   * Moreover, previous stages will have checked that this is the instantiation form.
   * <pre>
   *   //So this type of usage
   *   someVar <- List() of String
   *
   *   //Rather than just 'List of String' // note the omission of '()'
   * </pre>
   */
  private void resolveByParameterisedTypeOrError(final CallSymbol callSymbol, final EK9Parser.CallContext ctx) {

    final var symbol = (ScopedSymbol) symbolFromContextOrError.apply(ctx.parameterisedType());

    if (symbol != null) {
      callSymbol.setFormOfDeclarationCall(true);

      // Match the pattern from ResolveIdentifierReferenceCallOrError.checkGenericConstructionOrInvocation
      // (lines 177-182) to resolve the specific constructor MethodSymbol instead of just the aggregate.
      // Only do this when paramExpression is present (i.e., we have actual constructor call with '()')
      // Note: paramExpression is nested inside parameterisedType, not directly under call context
      if (symbol instanceof IAggregateSymbol asParameterisedAggregate
          && ctx.parameterisedType().paramExpression() != null) {
        // Extract call parameters (arguments passed to the constructor)
        final var callParams = symbolsFromParamExpression.apply(ctx.parameterisedType().paramExpression());

        // Resolve the specific constructor matching the call parameters
        final var resolvedConstructor = asParameterisedAggregate.resolveInThisScopeOnly(
            new MethodSymbolSearch(asParameterisedAggregate.getName()).setTypeParameters(callParams));

        if (resolvedConstructor.isPresent()) {
          // Set the specific constructor MethodSymbol (consistent with identifierReference path)
          callSymbol.setResolvedSymbolToCall((ScopedSymbol) resolvedConstructor.get());
        } else {
          // Fallback to aggregate if constructor not found (error will be caught in later phases)
          callSymbol.setResolvedSymbolToCall(symbol);
        }
      } else {
        // For generic functions (not aggregates), or type references without '()', use the symbol as-is
        callSymbol.setResolvedSymbolToCall(symbol);
      }

      pureProcessingInPureContextOrError.accept(callSymbol.getSourceToken(), callSymbol);
    }

  }

  /**
   * Resolve the dynamic function from the context.
   */
  private void resolveByDynamicFunctionOrError(final CallSymbol callSymbol, final EK9Parser.CallContext ctx) {

    final var symbol = (ScopedSymbol) symbolFromContextOrError.apply(ctx.dynamicFunctionDeclaration());

    if (symbol != null) {

      callSymbol.setFormOfDeclarationCall(true);
      callSymbol.setResolvedSymbolToCall(symbol);
      //Now this is a declaration (creation) of a dynamic function - it is not the calling of the dynamic function.
      //So do we consider the creation of a dynamic function pure? It does not really have any side effects.
      //Moreover, it cannot have any side effects, can it? The capture of variables and their calls is done before the
      //dynamic function is created and the values passed in and referenced.
      //Always consider this pure - as it is just creating a new function - but not actually calling it.
      callSymbol.setMarkedPure(true);
      pureProcessingInPureContextOrError.accept(callSymbol.getSourceToken(), callSymbol);

    }
  }

  /**
   * This can only be 'this' or 'super'.
   * So we're looking for a Constructor on the type or a constructor on the super type.
   */
  private void resolveByPrimaryReferenceOrError(final CallSymbol callSymbol, final EK9Parser.CallContext ctx) {

    //We force void here, because the constructor will return the type it is constructing
    //But when used via a 'call' like 'this()' or 'super()' we want to ensure it can only be used without assignment.
    final var symbol = thisOrSuperCallOrError.apply(ctx);

    if (symbol != null) {
      callSymbol.setResolvedSymbolToCall(symbol);
      callSymbol.setType(symbolsAndScopes.getEk9Types().ek9Void());
      pureProcessingInPureContextOrError.accept(callSymbol.getSourceToken(), callSymbol);
    }

  }

  /**
   * Now we'd be getting something back from another call.
   * We need to check that it is possible to make that call with the parameters provided.
   * Caters for 'straightToResult1 <- HigherFunctionOne()("Steve")'
   * i.e. where you call a higher function/method, and it returns a function, and you call that.
   */
  private void resolveByCallOrError(final CallSymbol callSymbol, final EK9Parser.CallContext ctx) {

    final var callParams = symbolsFromParamExpression.apply(ctx.paramExpression());
    final var callIdentifier = symbolFromContextOrError.apply(ctx.call());

    if (callIdentifier != null) {
      final var symbol = functionDelegateOrError.apply(
          new DelegateFunctionData(new Ek9Token(ctx.call().start), callIdentifier, callParams));

      if (symbol != null) {
        callSymbol.setResolvedSymbolToCall(symbol);
        pureProcessingInPureContextOrError.accept(callSymbol.getSourceToken(), callSymbol);
      }
    } else {
      AssertValue.fail("Expecting call to be at least present" + ctx.start.getLine());
    }

  }
}

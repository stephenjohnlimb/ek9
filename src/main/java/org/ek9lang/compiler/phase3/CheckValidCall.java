package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.support.AccessGenericInGeneric;
import org.ek9lang.compiler.support.CheckParameterizationComplete;
import org.ek9lang.compiler.support.SymbolFactory;
import org.ek9lang.compiler.symbols.CallSymbol;
import org.ek9lang.compiler.symbols.ScopedSymbol;
import org.ek9lang.compiler.tokenizer.Ek9Token;
import org.ek9lang.core.AssertValue;

/**
 * Designed to deal with this following part of the EK9 grammar.
 * <pre>
 *     identifierReference paramExpression - function/method/constructor/delegate with optional params
 *     | parameterisedType - the parameterization of a generic type, ie List of String for example
 *     | primaryReference paramExpression - this or super
 *     | dynamicFunctionDeclaration
 *     | call paramExpression - a bit weird but supports someHigherFunction()("Call what was returned")
 * </pre>
 */
final class CheckValidCall extends RuleSupport implements Consumer<EK9Parser.CallContext> {

  private final CheckParameterizationComplete checkParameterizationComplete = new CheckParameterizationComplete();
  private final ResolveThisSuperCallOrError resolveThisSuperCallOrError;
  private final ResolveIdentifierReferenceCallOrError resolveIdentifierReferenceCallOrError;
  private final SymbolsFromParamExpression symbolsFromParamExpression;
  private final SymbolFromContextOrError symbolFromContextOrError;
  private final CheckValidFunctionDelegateOrError checkValidFunctionDelegateOrError;
  private final AccessGenericInGeneric accessGenericInGeneric;

  /**
   * Lookup a pre-recorded 'call', now resolve what it is supposed to call and set its type.
   */
  CheckValidCall(final SymbolAndScopeManagement symbolAndScopeManagement,
                 final SymbolFactory symbolFactory,
                 final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
    this.resolveThisSuperCallOrError =
        new ResolveThisSuperCallOrError(symbolAndScopeManagement, errorListener);
    this.resolveIdentifierReferenceCallOrError =
        new ResolveIdentifierReferenceCallOrError(symbolAndScopeManagement, symbolFactory, errorListener);
    this.symbolsFromParamExpression =
        new SymbolsFromParamExpression(symbolAndScopeManagement, errorListener);
    this.symbolFromContextOrError =
        new SymbolFromContextOrError(symbolAndScopeManagement, errorListener);
    this.checkValidFunctionDelegateOrError =
        new CheckValidFunctionDelegateOrError(symbolAndScopeManagement, errorListener);
    this.accessGenericInGeneric =
        new AccessGenericInGeneric(symbolAndScopeManagement);
  }

  @Override
  public void accept(final EK9Parser.CallContext ctx) {
    var existingCallSymbol = symbolAndScopeManagement.getRecordedSymbol(ctx);
    if (existingCallSymbol instanceof CallSymbol callSymbol) {
      resolveToBeCalled(callSymbol, ctx);
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
  private void resolveToBeCalled(CallSymbol callSymbol, final EK9Parser.CallContext ctx) {
    ScopedSymbol symbol = null;
    boolean formOfDeclaration = false;
    boolean forceVoidReturnType = false;
    if (ctx.identifierReference() != null) {
      symbol = resolveByIdentifierReference(ctx);
      //Now this is where the developer has written 'l as List of String : List()'
      //Or fun as SomeFunction of Integer: SomeFunction().
      if (symbol != null) {
        //We must also cater for generics within generics.
        //Just checking isGenericInNature is not enough when used within a generic type.
        var genericInGenericData = accessGenericInGeneric.apply(symbol);
        if (genericInGenericData.isPresent()) {
          formOfDeclaration = !checkParameterizationComplete.test(genericInGenericData.get());
        } else {
          formOfDeclaration = symbol.isGenericInNature();
        }
      }
    } else if (ctx.parameterisedType() != null) {
      symbol = resolveByParameterisedType(ctx);
      if (symbol != null) {
        formOfDeclaration = true;
      }

    } else if (ctx.primaryReference() != null) {
      symbol = resolveByPrimaryReference(ctx);
      //We force void here, because the constructor will return the type it is constructing
      //But when used via a 'call' like 'this()' or 'super()' we want to ensure it can only be used without assignment.
      forceVoidReturnType = true;
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
      if (forceVoidReturnType) {
        callSymbol.setType(symbolAndScopeManagement.getEk9Types().ek9Void());
      }
    }
  }

  /**
   * This can be quite a few different 'types' of identifierReference.
   * So the processing in here is quite detailed with quite a few combinations and paths.
   */
  private ScopedSymbol resolveByIdentifierReference(EK9Parser.CallContext ctx) {
    return resolveIdentifierReferenceCallOrError.apply(ctx);
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
    return resolveThisSuperCallOrError.apply(ctx);
  }

  /**
   * Now we'd be getting something back from another call.
   * We need to check that it is possible to make that call with the parameters provided.
   * Caters for 'straightToResult1 <- HigherFunctionOne()("Steve")'
   * i.e. where you call a higher function/method and it returns a function and you call that.
   */
  private ScopedSymbol resolveByCall(EK9Parser.CallContext ctx) {
    var callParams = symbolsFromParamExpression.apply(ctx.paramExpression());
    var callIdentifier = symbolFromContextOrError.apply(ctx.call());
    if (callIdentifier != null) {
      return checkValidFunctionDelegateOrError.apply(
          new DelegateFunctionCheckData(new Ek9Token(ctx.call().start), callIdentifier, callParams));
    } else {
      AssertValue.fail("Expecting call to be at least present" + ctx.start.getLine());
    }
    return null;
  }
}

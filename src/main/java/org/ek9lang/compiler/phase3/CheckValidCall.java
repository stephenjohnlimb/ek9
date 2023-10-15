package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
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
 * This is one of the trickiest bits, because we want a simple syntax for the EK9 developer.
 * But it means that something(...) could mean quite a few things.
 * <pre>
 *     identifierReference paramExpression - function/method/constructor/delegate with optional params
 *     | parameterisedType - the parameterization of a generic type, ie List of String for example
 *     | primaryReference paramExpression - this or super
 *     | dynamicFunctionDeclaration
 *     | call paramExpression - a bit weird but supports someHigherFunction()("Call what was returned")
 * </pre>
 */
final class CheckValidCall extends TypedSymbolAccess implements Consumer<EK9Parser.CallContext> {
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
    //Note that we make an untyped check to get the call - because until we resolve
    //the thing we're going to call we won't know that type this existingCallSymbol is.
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
    if (ctx.identifierReference() != null) {
      resolveByIdentifierReference(callSymbol, ctx);
    } else if (ctx.parameterisedType() != null) {
      resolveByParameterisedType(callSymbol, ctx);
    } else if (ctx.primaryReference() != null) {
      resolveByPrimaryReference(callSymbol, ctx);
    } else if (ctx.dynamicFunctionDeclaration() != null) {
      resolveByDynamicFunctionDeclaration(callSymbol, ctx);
    } else if (ctx.call() != null) {
      resolveByCall(callSymbol, ctx);
    } else {
      AssertValue.fail("Expecting finite set of operations on call " + ctx.start.getLine());
    }
  }

  /**
   * This can be quite a few different 'types' of identifierReference.
   * So the processing in here is quite detailed with quite a few combinations and paths.
   */
  private void resolveByIdentifierReference(CallSymbol callSymbol, final EK9Parser.CallContext ctx) {
    var symbol = resolveIdentifierReferenceCallOrError.apply(ctx);
    //Now this is where the developer has written 'l as List of String : List()'
    //Or fun as SomeFunction of Integer: SomeFunction().
    if (symbol != null) {
      //We must also cater for generics within generics.
      //Just checking isGenericInNature is not enough when used within a generic type.
      var genericInGenericData = accessGenericInGeneric.apply(symbol);
      if (genericInGenericData.isPresent()) {
        callSymbol.setFormOfDeclarationCall(!checkParameterizationComplete.test(genericInGenericData.get()));
      } else {
        callSymbol.setFormOfDeclarationCall(symbol.isGenericInNature());
      }
      callSymbol.setResolvedSymbolToCall(symbol);
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
  private void resolveByParameterisedType(CallSymbol callSymbol, final EK9Parser.CallContext ctx) {

    var symbol = (ScopedSymbol) symbolFromContextOrError.apply(ctx.parameterisedType());
    if (symbol != null) {
      callSymbol.setFormOfDeclarationCall(true);
      callSymbol.setResolvedSymbolToCall(symbol);
    }
  }

  /**
   * Resolve the dynamic function from the context.
   */
  private void resolveByDynamicFunctionDeclaration(CallSymbol callSymbol, final EK9Parser.CallContext ctx) {
    var symbol = (ScopedSymbol) symbolFromContextOrError.apply(ctx.dynamicFunctionDeclaration());
    if (symbol != null) {
      callSymbol.setFormOfDeclarationCall(true);
      callSymbol.setResolvedSymbolToCall(symbol);
    }
  }

  /**
   * This can only be 'this' or 'super'.
   * So we're looking for a Constructor on the type or a constructor on the super type.
   */
  private void resolveByPrimaryReference(CallSymbol callSymbol, final EK9Parser.CallContext ctx) {
    //We force void here, because the constructor will return the type it is constructing
    //But when used via a 'call' like 'this()' or 'super()' we want to ensure it can only be used without assignment.
    var symbol = resolveThisSuperCallOrError.apply(ctx);
    if (symbol != null) {
      callSymbol.setResolvedSymbolToCall(symbol);
      callSymbol.setType(symbolAndScopeManagement.getEk9Types().ek9Void());
    }
  }

  /**
   * Now we'd be getting something back from another call.
   * We need to check that it is possible to make that call with the parameters provided.
   * Caters for 'straightToResult1 <- HigherFunctionOne()("Steve")'
   * i.e. where you call a higher function/method, and it returns a function, and you call that.
   */
  private void resolveByCall(CallSymbol callSymbol, final EK9Parser.CallContext ctx) {
    var callParams = symbolsFromParamExpression.apply(ctx.paramExpression());
    var callIdentifier = symbolFromContextOrError.apply(ctx.call());
    if (callIdentifier != null) {
      var symbol = checkValidFunctionDelegateOrError.apply(
          new DelegateFunctionCheckData(new Ek9Token(ctx.call().start), callIdentifier, callParams));
      if (symbol != null) {
        callSymbol.setResolvedSymbolToCall(symbol);
      }
    } else {
      AssertValue.fail("Expecting call to be at least present" + ctx.start.getLine());
    }
  }
}

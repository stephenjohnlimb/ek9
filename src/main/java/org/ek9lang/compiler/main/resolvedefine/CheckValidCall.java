package org.ek9lang.compiler.main.resolvedefine;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.antlr.v4.runtime.ParserRuleContext;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.main.phases.definition.SymbolAndScopeManagement;
import org.ek9lang.compiler.symbol.CallSymbol;
import org.ek9lang.compiler.symbol.IAggregateSymbol;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.ScopedSymbol;
import org.ek9lang.compiler.symbol.support.SymbolTypeExtractor;
import org.ek9lang.compiler.symbol.support.search.MethodSymbolSearch;
import org.ek9lang.compiler.symbol.support.search.MethodSymbolSearchResult;
import org.ek9lang.core.exception.AssertValue;

/**
 * TODO lots of tidying up.
 */
public class CheckValidCall implements Consumer<EK9Parser.CallContext> {
  private final SymbolAndScopeManagement symbolAndScopeManagement;

  private final ErrorListener errorListener;

  private final SymbolTypeExtractor symbolTypeExtractor = new SymbolTypeExtractor();

  /**
   * Lookup a pre-recorded 'call', now resolve what it is supposed to call and set it's type.
   */
  public CheckValidCall(final SymbolAndScopeManagement symbolAndScopeManagement,
                        final ErrorListener errorListener) {
    this.symbolAndScopeManagement = symbolAndScopeManagement;
    this.errorListener = errorListener;
  }


  @Override
  public void accept(final EK9Parser.CallContext ctx) {
    var symbol = determineSymbolToRecord(ctx);
    if (symbol != null) {
      symbolAndScopeManagement.recordSymbol(symbol, ctx);
    }
  }

  /**
   * Gets the appropriate symbol to register against this context.
   */
  private ISymbol determineSymbolToRecord(final EK9Parser.CallContext ctx) {
    var existingCallSymbol = symbolAndScopeManagement.getRecordedSymbol(ctx);
    if (existingCallSymbol instanceof CallSymbol callSymbol) {
      if (existingCallSymbol != null) {
        var toBeCalled = resolveTobeCalled(ctx);
        if (toBeCalled != null) {
          callSymbol.setResolvedSymbolToCall(toBeCalled);
          callSymbol.setType(toBeCalled.getType());
        }
        System.out.println(
            "Have an existing call symbol [" + existingCallSymbol + "] [" + existingCallSymbol.getType() + "]");
      }
    } else {
      AssertValue.fail("ValidateCall expecting a CallSymbol type");
    }

    return existingCallSymbol;
  }

  /**
   * This is where it is important to resolve something to be called.
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
  private ScopedSymbol resolveTobeCalled(final EK9Parser.CallContext ctx) {
    ScopedSymbol symbol = null;
    if (ctx.identifierReference() != null) {
      symbol = resolveByIdentifierReference(ctx);
    } else if (ctx.parameterisedType() != null) {
      symbol = (ScopedSymbol) getSymbolFromContext(ctx.parameterisedType());
    } else if (ctx.primaryReference() != null) {
      symbol = resolveByPrimaryReference(ctx);
    } else if (ctx.dynamicFunctionDeclaration() != null) {
      symbol = (ScopedSymbol) getSymbolFromContext(ctx.dynamicFunctionDeclaration());
    } else if (ctx.call() != null) {
      symbol = resolveByCall(ctx);
    } else {
      AssertValue.fail("Expecting finite set of operations on call " + ctx.start.getLine());
    }
    return symbol;
  }

  private ScopedSymbol resolveByIdentifierReference(EK9Parser.CallContext ctx) {
    var callParams = getParamExpressionSymbols(ctx.paramExpression());
    //function/method/constructor/delegate with optional params
    var callIdentifier = getSymbolFromContext(ctx.identifierReference());
    System.out.println("The Identifier for the call is " + callIdentifier + " with params " + callParams + " "
        + callIdentifier.getGenus().getDescription());


    //TODO more but let's see what happens just for 'class' for now.
    if (callIdentifier instanceof IAggregateSymbol aggregate) {
      var resolved = checkForMethodOnAggregate(aggregate, callIdentifier.getName(), callParams);
      System.out.println("The resolved item is [" + resolved + "]");
      return (ScopedSymbol) resolved;
    }
    //So we now have the combinations of what is needed, now it just depends on 'what it is'
    return null;
  }

  /**
   * This can only be 'this' or 'super'.
   * So we're looking for a Constructor on the type or a constructor on the super type.
   */
  private ScopedSymbol resolveByPrimaryReference(EK9Parser.CallContext ctx) {
    var callParams = getParamExpressionSymbols(ctx.paramExpression());
    var callIdentifier = getSymbolFromContext(ctx.primaryReference());
    return null;
  }

  /**
   * Now we'd be getting something back from another call.
   * We need to check that it is possible to make that call with the parameters provided.
   */
  private ScopedSymbol resolveByCall(EK9Parser.CallContext ctx) {
    var callParams = getParamExpressionSymbols(ctx.paramExpression());
    var callIdentifier = getSymbolFromContext(ctx.call());

    return null;
  }

  /**
   * Go through and get the symbols from the expressions in the parameters.
   * TODO move out to a separate function.
   */
  private List<ISymbol> getParamExpressionSymbols(final EK9Parser.ParamExpressionContext ctx) {
    return ctx.expressionParam()
        .stream()
        .map(expressionParam -> expressionParam.expression())
        .map(this::getSymbolFromContext)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  private ISymbol checkForMethodOnAggregate(final IAggregateSymbol aggregate,
                                            final String methodName,
                                            final List<ISymbol> parameters) {

    //TODO pull out to a function that can do all this and issue correct error messages.
    var paramTypes = symbolTypeExtractor.apply(parameters);
    //maybe earlier types were not defined by the ek9 developer so let's not look at it would be misleading.
    if (parameters.size() == paramTypes.size()) {
      var search = new MethodSymbolSearch(methodName).setTypeParameters(paramTypes);
      var results = aggregate.resolveMatchingMethods(search, new MethodSymbolSearchResult());
      if (results.isSingleBestMatchPresent() && results.getSingleBestMatchSymbol().isPresent()) {
        return results.getSingleBestMatchSymbol().get();
      }
    }
    return null;
  }

  private ISymbol getSymbolFromContext(final ParserRuleContext ctx) {
    var resolved = symbolAndScopeManagement.getRecordedSymbol(ctx);
    if (resolved == null) {
      errorListener.semanticError(ctx.start, "", ErrorListener.SemanticClassification.NOT_RESOLVED);
    }
    return resolved;
  }
}

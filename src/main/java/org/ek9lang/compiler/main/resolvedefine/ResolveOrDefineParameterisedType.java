package org.ek9lang.compiler.main.resolvedefine;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.main.phases.definition.SymbolAndScopeManagement;
import org.ek9lang.compiler.symbol.AggregateSymbol;
import org.ek9lang.compiler.symbol.FunctionSymbol;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.support.SymbolFactory;
import org.ek9lang.compiler.symbol.support.search.AnySymbolSearch;
import org.ek9lang.core.exception.AssertValue;

/**
 * This is a sort of hybrid resolver or definer function.
 * Used when it is necessary to create concrete parameterised types.
 * Such as List of String, Dict of (Integer, Date) or even things like:
 * Dict of(Integer, List of Date) for example.
 * So not used in the definition of the generic type itself. But used when the developer want to
 * use an exising/created Template/Generic type with specific parameters.
 */
public class ResolveOrDefineParameterisedType implements Function<EK9Parser.TypeDefContext, Optional<ISymbol>> {

  private final SymbolAndScopeManagement symbolAndScopeManagement;
  private final SymbolFactory symbolFactory;
  private final ErrorListener errorListener;
  private final boolean errorIfNotDefinedOrResolved;

  /**
   * A bit of a complex function constructor - for a function.
   * But then this is a bit of a beast of a function.
   */
  public ResolveOrDefineParameterisedType(final SymbolAndScopeManagement symbolAndScopeManagement,
                                          final SymbolFactory symbolFactory, final ErrorListener errorListener,
                                          final boolean errorIfNotDefinedOrResolved) {
    AssertValue.checkNotNull("symbolAndScopeManagement cannot be null", symbolAndScopeManagement);
    AssertValue.checkNotNull("symbolFactory cannot be null", symbolFactory);
    AssertValue.checkNotNull("errorListener cannot be null", errorListener);

    this.symbolAndScopeManagement = symbolAndScopeManagement;
    this.symbolFactory = symbolFactory;
    this.errorListener = errorListener;
    this.errorIfNotDefinedOrResolved = errorIfNotDefinedOrResolved;
  }

  @Override
  @SuppressWarnings("java:S125")
  public Optional<ISymbol> apply(EK9Parser.TypeDefContext ctx) {

    if (ctx == null) {
      return Optional.empty();
    }

    //Now this is a bit nasty because the grammar is recursive.
    //So this code is also recursive.
    // So this allows things like 'Dict of(Integer, List of Date)' etc. but infinitely deep
    /*
    typeDef
    : identifierReference
    | parameterisedType
    ;
    parameterisedType
    : identifierReference OF LPAREN parameterisedArgs RPAREN
    | identifierReference OF typeDef
    ;
    parameterisedArgs
    : typeDef (COMMA typeDef)*
    ;
    */

    return attemptToTypeVariable(ctx);
  }

  private Optional<ISymbol> attemptToTypeVariable(final EK9Parser.TypeDefContext ctx) {

    //The Simple case
    if (ctx.identifierReference() != null) {
      return resolveSimpleTypeByIdentifierReference(ctx.identifierReference());
    }

    //The Next most complex - a parameterisedType, with either:
    //either way it's back around the recursion via these methods to this same method with a different typeDef context
    if (ctx.parameterisedType() != null) {
      //Now we will attempt a simple resolution of the identifierReference
      var resolvedGenericType = resolveSimpleTypeByIdentifierReference(ctx.parameterisedType().identifierReference());

      if (resolvedGenericType.isPresent()) {
        //So as that resolved lets now get any parameterizing parameters.
        return resolveSimpleTypeByIdentifierReference(resolvedGenericType.get(), ctx.parameterisedType());
      }
    }

    return Optional.empty();
  }

  private Optional<ISymbol> resolveSimpleTypeByIdentifierReference(final EK9Parser.IdentifierReferenceContext ctx) {
    var ofType = ctx.getText();
    var resolved = symbolAndScopeManagement.getTopScope().resolve(new AnySymbolSearch(ofType));
    if (resolved.isEmpty() && errorIfNotDefinedOrResolved) {
      errorListener.semanticError(ctx.start, "",
          ErrorListener.SemanticClassification.TYPE_NOT_RESOLVED);
    }
    return resolved;
  }

  private Optional<ISymbol> resolveSimpleTypeByIdentifierReference(final ISymbol resolvedGenericType,
                                                                   final EK9Parser.ParameterisedTypeContext ctx) {
    //So trigger the recursive call back to the top most method.
    //This is just a single parameterizing parameter.
    if (ctx.typeDef() != null) {
      var resolvedParameterisedType = attemptToTypeVariable(ctx.typeDef());
      if (resolvedParameterisedType.isPresent()) {
        return resolveOrDefine(resolvedGenericType, List.of(resolvedParameterisedType.get()));
      }
    } else if (ctx.parameterisedArgs() != null) {
      //This is for multiple parameterizing parameters.
      //Multiple type defs - so we need to try and get them all and hold in a list.
      var genericParameters = new ArrayList<ISymbol>();
      for (var typeDefCtx : ctx.parameterisedArgs().typeDef()) {
        //Multiple recursive calls back around the loop.
        var resolved = attemptToTypeVariable(typeDefCtx);
        resolved.ifPresent(genericParameters::add);
      }
      //Did we resolve them all? Only if we did
      if (genericParameters.size() == ctx.parameterisedArgs().typeDef().size()) {
        return resolveOrDefine(resolvedGenericType, genericParameters);
      }
    }
    return Optional.empty();
  }

  private Optional<ISymbol> resolveOrDefine(final ISymbol genericType, final List<ISymbol> parameterizingTypes) {
    if (genericType instanceof AggregateSymbol genericAggregateType) {
      var theType = symbolFactory.newParameterisedTypeSymbol(genericAggregateType, parameterizingTypes);
      return symbolAndScopeManagement.resolveOrDefine(theType);
    } else if (genericType instanceof FunctionSymbol genericFunction) {
      var theType = symbolFactory.newParameterisedFunctionSymbol(genericFunction, parameterizingTypes);
      return symbolAndScopeManagement.resolveOrDefine(theType);
    }

    return Optional.empty();
  }
}

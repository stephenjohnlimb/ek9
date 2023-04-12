package org.ek9lang.compiler.main.resolvedefine;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.antlr.v4.runtime.Token;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.main.phases.definition.SymbolAndScopeManagement;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.PossibleGenericSymbol;
import org.ek9lang.compiler.symbol.support.SymbolFactory;
import org.ek9lang.compiler.symbol.support.search.AnyTypeSymbolSearch;
import org.ek9lang.core.exception.AssertValue;

/**
 * This is a sort of hybrid resolver or definer function.
 * Used when it is necessary to create concrete parameterised types.
 * Such as List of String, Dict of (Integer, Date) or even things like:
 * Dict of(Integer, List of Date) for example.
 * So not used in the definition of the generic type itself. But used when the developer want to
 * use an exising/created Template/Generic type with specific parameters.
 * This also works for generic Functions like 'Supplier of Integer' (but return type is not yet set).
 * As we need multiple entry points into this from different contexts, this abstract base has all the code.
 * typeDef, parameterisedType, parameterisedArgs and identifierReference can be used in a recursive manner
 * and so this code calls itself quite a bit.
 * But it may get called with parameterization params that are also just parameters lie S and T for example.
 */
public abstract class ResolveOrDefineTypes {

  private final SymbolAndScopeManagement symbolAndScopeManagement;
  private final SymbolFactory symbolFactory;
  private final ErrorListener errorListener;
  private final boolean errorIfNotDefinedOrResolved;

  /**
   * A bit of a complex function constructor - for a function.
   * But then this is a bit of a beast of a function.
   */
  protected ResolveOrDefineTypes(final SymbolAndScopeManagement symbolAndScopeManagement,
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

  protected Optional<ISymbol> resolveTypeByTypeDef(final EK9Parser.TypeDefContext ctx) {

    //The Simple case
    if (ctx.identifierReference() != null) {
      return resolveSimpleTypeByIdentifierReference(ctx.identifierReference());
    }

    //The Next most complex - a parameterisedType
    //either way it's back around the recursion via these methods to this same method with a different typeDef context
    if (ctx.parameterisedType() != null) {
      //Now we will attempt a simple resolution of the identifierReference
      return resolveTypeByParameterizedType(ctx.parameterisedType());
    }

    return Optional.empty();
  }

  protected Optional<ISymbol> resolveTypeByParameterizedType(final EK9Parser.ParameterisedTypeContext ctx) {
    var resolvedGenericType = resolveSimpleTypeByIdentifierReference(ctx.identifierReference());

    if (resolvedGenericType.isPresent()) {
      //So as that resolved lets now get any parameterizing parameters.
      return resolveSimpleTypeByIdentifierReference(resolvedGenericType.get(), ctx);
    }
    return Optional.empty();
  }

  protected Optional<ISymbol> resolveSimpleTypeByIdentifierReference(final EK9Parser.IdentifierReferenceContext ctx) {
    var ofType = ctx.getText();
    var scope = symbolAndScopeManagement.getTopScope();
    var resolved = scope.resolve(new AnyTypeSymbolSearch(ofType));
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
      var resolvedParameterisedType = resolveTypeByTypeDef(ctx.typeDef());
      if (resolvedParameterisedType.isPresent()) {
        return resolveOrDefine(ctx.typeDef().start, resolvedGenericType, List.of(resolvedParameterisedType.get()));
      }
    } else if (ctx.parameterisedArgs() != null) {
      //This is for multiple parameterizing parameters.
      //Multiple type defs - so we need to try and get them all and hold in a list.
      var genericParameters = new ArrayList<ISymbol>();
      for (var typeDefCtx : ctx.parameterisedArgs().typeDef()) {
        //Multiple recursive calls back around the loop.
        var resolved = resolveTypeByTypeDef(typeDefCtx);
        resolved.ifPresent(genericParameters::add);
      }
      //Did we resolve them all? Only if we did
      if (genericParameters.size() == ctx.parameterisedArgs().typeDef().size()) {
        return resolveOrDefine(ctx.parameterisedArgs().start, resolvedGenericType, genericParameters);
      }
    }
    return Optional.empty();
  }

  private Optional<ISymbol> resolveOrDefine(final Token token, final ISymbol genericType,
                                            final List<ISymbol> parameterizingTypes) {

    if (genericType instanceof PossibleGenericSymbol genericTypeSymbol) {

      if (!genericType.isGenericInNature()) {
        if (errorIfNotDefinedOrResolved) {
          errorListener.semanticError(token, "cannot be used to parameterize '" + genericType.getFriendlyName() + "':",
              ErrorListener.SemanticClassification.NOT_A_TEMPLATE);
        }
        return Optional.empty();
      }

      //It is generic in nature, but do the number of parameterizing types and number of types the generic type
      //need match up? Always give an error for this.
      var acceptsNParameters = genericTypeSymbol.getAnyConceptualTypeParameters().size();
      var providedWithNParameters = parameterizingTypes.size();
      if (acceptsNParameters != providedWithNParameters) {
        errorListener.semanticError(token, "'"
                + providedWithNParameters + "' parameters were supplied but '"
                + genericType.getFriendlyName() + "' requires '"
                + acceptsNParameters + "':",
            ErrorListener.SemanticClassification.GENERIC_TYPE_OR_FUNCTION_PARAMETERS_INCORRECT);
        return Optional.empty();
      }

      var theType = symbolFactory.newParameterisedSymbol(genericTypeSymbol, parameterizingTypes);
      return symbolAndScopeManagement.resolveOrDefine(theType);
    }
    return Optional.empty();
  }
}

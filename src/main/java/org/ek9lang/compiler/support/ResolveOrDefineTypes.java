package org.ek9lang.compiler.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.search.AnyTypeSymbolSearch;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.PossibleGenericSymbol;
import org.ek9lang.compiler.tokenizer.Ek9Token;

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
public abstract class ResolveOrDefineTypes extends ResolverOrDefiner {


  /**
   * A bit of a complex function constructor - for a function.
   * But then this is a bit of a beast of a function.
   */
  protected ResolveOrDefineTypes(final SymbolAndScopeManagement symbolAndScopeManagement,
                                 final SymbolFactory symbolFactory, final ErrorListener errorListener,
                                 final boolean errorIfNotDefinedOrResolved) {

    super(symbolAndScopeManagement, symbolFactory, errorListener, errorIfNotDefinedOrResolved);
  }

  protected Optional<ISymbol> resolveTypeByTypeDef(final EK9Parser.TypeDefContext ctx) {

    Optional<ISymbol> rtn = Optional.empty();
    //The Simple case (ish), but need to check that the type returned is not a template/generic type
    //because no parameters have been supplied. If this is the case then error and no-resolution (i.e. return empty).
    if (ctx.identifierReference() != null) {
      rtn = resolveSimpleTypeByIdentifierReference(ctx.identifierReference());
      if (rtn.isPresent() && rtn.get() instanceof PossibleGenericSymbol maybeGenericType
          && maybeGenericType.isGenericInNature()) {
        errorListener.semanticError(ctx.start, "'" + maybeGenericType.getFriendlyName() + "':",
            ErrorListener.SemanticClassification.TEMPLATE_TYPE_REQUIRES_PARAMETERIZATION);
        return Optional.empty();
      }

    } else if (ctx.parameterisedType() != null) {
      //The Next most complex - a parameterisedType
      //either way it's back around the recursion via these methods to this same method with a different typeDef context
      //Now we will attempt a simple resolution of the identifierReference
      rtn = resolveTypeByParameterizedType(ctx.parameterisedType());
    }

    return rtn;
  }

  protected Optional<ISymbol> resolveTypeByParameterizedType(final EK9Parser.ParameterisedTypeContext ctx) {
    var resolvedGenericType = resolveTypeByIdentifierReference(ctx.identifierReference());

    if (resolvedGenericType.isPresent()) {
      //So as that resolved lets now get any parameterizing parameters.
      return resolveSimpleTypeByIdentifierReference(resolvedGenericType.get(), ctx);
    }
    return Optional.empty();
  }

  protected Optional<ISymbol> resolveTypeByIdentifierReference(final EK9Parser.IdentifierReferenceContext ctx) {
    var ofType = ctx.getText();
    var scope = symbolAndScopeManagement.getTopScope();
    var resolved = scope.resolve(new AnyTypeSymbolSearch(ofType));
    if (resolved.isEmpty() && errorIfNotDefinedOrResolved) {
      errorListener.semanticError(ctx.start, "",
          ErrorListener.SemanticClassification.TYPE_NOT_RESOLVED);
    }
    return resolved;
  }

  protected Optional<ISymbol> resolveSimpleTypeByIdentifierReference(final EK9Parser.IdentifierReferenceContext ctx) {
    return resolveTypeByIdentifierReference(ctx);
  }

  private Optional<ISymbol> resolveSimpleTypeByIdentifierReference(final ISymbol resolvedGenericType,
                                                                   final EK9Parser.ParameterisedTypeContext ctx) {
    //So trigger the recursive call back to the top most method.
    //This is just a single parameterizing parameter.
    if (ctx.typeDef() != null) {
      var resolvedParameterisedType = resolveTypeByTypeDef(ctx.typeDef());
      if (resolvedParameterisedType.isPresent()) {
        var toResolveOrDefine = new ParameterisedTypeData(new Ek9Token(ctx.typeDef().start), resolvedGenericType,
            List.of(resolvedParameterisedType.get()));
        return resolveOrDefine(toResolveOrDefine);
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
        var toResolveOrDefine =
            new ParameterisedTypeData(new Ek9Token(ctx.parameterisedArgs().start), resolvedGenericType,
                genericParameters);
        return resolveOrDefine(toResolveOrDefine);
      }
    }
    return Optional.empty();
  }
}

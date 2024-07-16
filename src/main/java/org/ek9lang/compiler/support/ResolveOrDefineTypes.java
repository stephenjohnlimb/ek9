package org.ek9lang.compiler.support;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.TEMPLATE_TYPE_REQUIRES_PARAMETERIZATION;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.TYPE_NOT_RESOLVED;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.search.AnyTypeSymbolSearch;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.PossibleGenericSymbol;
import org.ek9lang.compiler.tokenizer.IToken;

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
  protected ResolveOrDefineTypes(final SymbolsAndScopes symbolsAndScopes,
                                 final SymbolFactory symbolFactory,
                                 final ErrorListener errorListener,
                                 final boolean errorIfNotDefinedOrResolved) {

    super(symbolsAndScopes, symbolFactory, errorListener, errorIfNotDefinedOrResolved);

  }

  /**
   * Resolves the type for a type def, can trigger recursion.
   * <pre>
   *   typeDef
   *     : identifierReference
   *     | parameterisedType
   *     ;
   * </pre>
   */
  protected Optional<ISymbol> resolveTypeByTypeDef(final IToken triggerToken, final EK9Parser.TypeDefContext ctx) {

    if (ctx.parameterisedType() != null) {
      return resolveTypeByParameterizedType(triggerToken, ctx.parameterisedType());
    }

    final var rtn = resolveSimpleTypeByIdentifierReference(ctx.identifierReference());

    if (rtn.isPresent() && rtn.get() instanceof PossibleGenericSymbol maybeGenericType
        && maybeGenericType.isGenericInNature()) {

      final var msg = "'" + maybeGenericType.getFriendlyName() + "':";
      errorListener.semanticError(ctx.start, msg, TEMPLATE_TYPE_REQUIRES_PARAMETERIZATION);

      return Optional.empty();
    }

    return rtn;
  }

  protected Optional<ISymbol> resolveTypeByParameterizedType(final IToken triggerToken,
                                                             final EK9Parser.ParameterisedTypeContext ctx) {

    final var resolvedGenericType = resolveTypeByIdentifierReference(ctx.identifierReference());

    if (resolvedGenericType.isPresent()) {
      //So as that resolved lets now get any parameterizing parameters.
      return resolveSimpleTypeByIdentifierReference(triggerToken, resolvedGenericType.get(), ctx);
    }

    return Optional.empty();
  }

  protected Optional<ISymbol> resolveSimpleTypeByIdentifierReference(final EK9Parser.IdentifierReferenceContext ctx) {

    return resolveTypeByIdentifierReference(ctx);
  }


  private Optional<ISymbol> resolveSimpleTypeByIdentifierReference(final IToken triggerToken,
                                                                   final ISymbol resolvedGenericType,
                                                                   final EK9Parser.ParameterisedTypeContext ctx) {

    //So trigger the recursive call back to the top most method.
    //This is just a single parameterizing parameter.
    if (ctx.typeDef() != null) {
      final var resolvedParameterisedType = resolveTypeByTypeDef(triggerToken, ctx.typeDef());

      if (resolvedParameterisedType.isPresent()) {
        final var toResolveOrDefine = new ParameterisedTypeData(triggerToken, resolvedGenericType,
            List.of(resolvedParameterisedType.get()));
        return resolveOrDefine(toResolveOrDefine);
      }
    } else if (ctx.parameterisedArgs() != null) {

      //This is for multiple parameterizing parameters.
      //Multiple type defs - so we need to try and get them all and hold in a list.
      final var genericParameters = new ArrayList<ISymbol>();
      for (var typeDefCtx : ctx.parameterisedArgs().typeDef()) {
        //Multiple recursive calls back around the loop.
        final var resolved = resolveTypeByTypeDef(triggerToken, typeDefCtx);
        resolved.ifPresent(genericParameters::add);
      }

      //Did we resolve them all? Only if we did
      if (genericParameters.size() == ctx.parameterisedArgs().typeDef().size()) {
        final var toResolveOrDefine =
            new ParameterisedTypeData(triggerToken, resolvedGenericType, genericParameters);
        return resolveOrDefine(toResolveOrDefine);
      }
    }

    return Optional.empty();
  }

  protected Optional<ISymbol> resolveTypeByIdentifierReference(final EK9Parser.IdentifierReferenceContext ctx) {

    final var forSymbolName = ctx.getText();
    final var scope = symbolsAndScopes.getTopScope();
    final var resolved = scope.resolve(new AnyTypeSymbolSearch(forSymbolName));

    if (resolved.isEmpty() && errorIfNotDefinedOrResolved) {
      errorListener.semanticError(ctx.start, "", TYPE_NOT_RESOLVED);
    }

    return resolved;
  }

}

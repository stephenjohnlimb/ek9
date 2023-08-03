package org.ek9lang.compiler.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import org.ek9lang.compiler.ResolvedOrDefineResult;
import org.ek9lang.compiler.symbols.FunctionSymbol;
import org.ek9lang.compiler.symbols.IScope;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.symbols.PossibleGenericSymbol;
import org.ek9lang.core.AssertValue;

/**
 * Accepts an aggregate or a function that has been parameterized with 'type arguments'.
 * It goes through the 'generic type' with its 'type parameters' and the 'type arguments'
 * supplied and searches for the conceptual types 'T' etc. in what has been defined in the
 * 'generic type' and clones the method or parameter (in case of a function) and the associated
 * returns if there are any. It then replaces the 'T' with the appropriate 'type argument'.
 * This does mutate the 'possibleGenericSymbol' passed in.
 */
public class TypeSubstitution implements UnaryOperator<PossibleGenericSymbol> {

  private final ParameterizedSymbolCreator creator = new ParameterizedSymbolCreator();

  /**
   * Deals with the conceptual mappings.
   */
  private final ConceptualLookupMapping conceptualLookupMapping = new ConceptualLookupMapping();

  /**
   * When cloning types if we encounter a cloned PossibleGenericSymbol must check if already held and use
   * that, or it must be defined so that it can be used through the application.
   */
  private final Function<PossibleGenericSymbol, ResolvedOrDefineResult> resolveOrDefine;

  public TypeSubstitution(final Function<PossibleGenericSymbol, ResolvedOrDefineResult> resolveOrDefine) {
    AssertValue.checkNotNull("resolveOrDefine cannot be null", resolveOrDefine);
    this.resolveOrDefine = resolveOrDefine;
  }

  @Override
  public PossibleGenericSymbol apply(final PossibleGenericSymbol parameterisedSymbol) {
    //mainly just preconditions
    AssertValue.checkNotNull("parameterisedSymbol cannot be null",
        parameterisedSymbol);
    AssertValue.checkFalse("parameterisedSymbol does not have any type arguments",
        parameterisedSymbol.getTypeParameterOrArguments().isEmpty());

    var theGenericType = parameterisedSymbol.getGenericType();

    AssertValue.checkTrue("parameterisedSymbol does not have a generic type",
        theGenericType.isPresent());
    AssertValue.checkFalse("theGenericType withing the parameterisedSymbol does not have any type arguments",
        theGenericType.get().getTypeParameterOrArguments().isEmpty());

    //OK now on to the processing.
    return doApply(parameterisedSymbol, theGenericType.get());
  }

  /**
   * The main entry point, once all the preconditions have been dealt with.
   * This function will be called just after a parameterized type/function has been created.
   * So it will be devoid of any methods, fields, etc.
   *
   * @param parameterisedSymbol The outer parameterised symbol
   * @param genericSymbol       The generic type this will be a parameterization of.
   * @return The now populated parameterised symbol
   */
  private PossibleGenericSymbol doApply(final PossibleGenericSymbol parameterisedSymbol,
                                        final PossibleGenericSymbol genericSymbol) {

    //This is how new types get recorded or if they exist already we just reuse them.
    //So this is important, if we have already created one of these it MUST be reused
    //This is because later stage of the compiler will augment, not just that type but the types it references
    //as type arguments. Indeed, it may itself be a type argument elsewhere.
    var result = resolveOrDefine.apply(parameterisedSymbol);
    if (!result.newlyDefined() && result.symbol().isPresent()) {
      return result.symbol().get();
    }

    //So it is newly defined.
    var resultingType = result.symbol();
    AssertValue.checkTrue("Result must be present", resultingType.isPresent());

    //Can safely cast.
    PossibleGenericSymbol rtnType = resultingType.get();

    var symbolsToClone = genericSymbol.getSymbolsForThisScope();
    var clonedSymbols = cloneWithEnclosingScope(symbolsToClone, parameterisedSymbol);
    //Now get the mapping of 'T' or whatever to actual parameters.
    var typeMapping = conceptualLookupMapping.apply(parameterisedSymbol.getTypeParameterOrArguments(),
        genericSymbol.getAnyConceptualTypeParameters());

    //Still unsure about references - maybe too early in processing
    processAnyReferences(genericSymbol, typeMapping);

    //Do the business of changing the types.
    replaceTypeParametersWithTypeArguments(rtnType, typeMapping, clonedSymbols);

    if (genericSymbol instanceof FunctionSymbol genericFunctionSymbol
        && parameterisedSymbol instanceof FunctionSymbol functionSymbol
        && genericFunctionSymbol.isReturningSymbolPresent()) {
      var clonedSymbol = genericFunctionSymbol.getReturningSymbol().clone(genericFunctionSymbol);
      substituteAsAppropriate(typeMapping, clonedSymbol);
      functionSymbol.setReturningSymbol(clonedSymbol);

    }
    return rtnType;
  }

  private void processAnyReferences(final PossibleGenericSymbol genericSymbol,
                                    final Map<ISymbol, ISymbol> typeMapping) {
    if (!genericSymbol.getGenericSymbolReferences().isEmpty()) {
      for (var genericSymbolReference : genericSymbol.getGenericSymbolReferences()) {
        var newType = getReplacementType(typeMapping, genericSymbolReference);
        AssertValue.checkNotNull("Expecting new type to be created", newType);
      }
    }
  }

  /**
   * Now we have a mapping of the 'type parameter' to the 'type argument', we can cycle through
   * all the symbols and replace the types are they are encountered on the cloned symbols.
   * But beware there could and probably will be generics of generics or generics, and we have to replace the
   * types to create new parameterised types as we go.
   */
  private void replaceTypeParametersWithTypeArguments(final PossibleGenericSymbol possibleGenericSymbol,
                                                      final Map<ISymbol, ISymbol> typeMapping, List<ISymbol> symbols) {
    symbols.forEach(symbol -> {
      if (symbol instanceof MethodSymbol methodSymbol) {
        if (methodSymbol.isConstructor()) {
          //Alter the name of the Method to match the name of the scope (i.e. class)
          //This is similar to Java where the name of the aggregate indicates a constructor.
          methodSymbol.setName(possibleGenericSymbol.getName());
          //Also need to alter the return type as a constructor.
          methodSymbol.setType(possibleGenericSymbol);
        }
        methodSymbol.getCallParameters().forEach(param -> substituteAsAppropriate(typeMapping, param));
        if (methodSymbol.isReturningSymbolPresent()) {
          substituteAsAppropriate(typeMapping, methodSymbol.getReturningSymbol());
        }
      }
      substituteAsAppropriate(typeMapping, symbol);
    });
  }

  private void substituteAsAppropriate(final Map<ISymbol, ISymbol> typeMapping, ISymbol value) {
    var paramType = value.getType();
    paramType.ifPresent(type -> value.setType(getReplacementTypeAsAppropriate(typeMapping, type)));
  }

  private ISymbol getReplacementTypeAsAppropriate(final Map<ISymbol, ISymbol> typeMapping, ISymbol forType) {
    //Have to be aware some types might be generic, others are just simple.
    if (forType instanceof PossibleGenericSymbol couldBeGeneric && couldBeGeneric.isGenericInNature()) {
      return getReplacementType(typeMapping, couldBeGeneric);
    } else if (typeMapping.containsKey(forType)) {
      return typeMapping.get(forType);
    }
    return forType;
  }

  private ISymbol getReplacementType(final Map<ISymbol, ISymbol> typeMapping,
                                     final PossibleGenericSymbol forType) {

    //This is a scenario where we have 'D1 of type X of type P' i.e. conceptual.
    //So if we are now to replace the 'P' with something we need to go all the way down!
    //because this could be a 'D1 of type (X, Y, Z) of type (P, Date, G2 of (P, P))

    var theGenericType = forType.getGenericType();
    AssertValue.checkTrue("Must have a generic type", theGenericType.isPresent());

    var genericType = theGenericType.get();

    var newTypeArguments = new ArrayList<ISymbol>();
    for (var typeArgument : forType.getTypeParameterOrArguments()) {
      var replacementType = getReplacementTypeAsAppropriate(typeMapping, typeArgument);
      newTypeArguments.add(replacementType);
    }
    //OK, so now we may have a new set of type arguments so as we are in 'generic world' we need to resolve or define.
    //So first create the outline

    var possibleNewParameterisedType = creator.apply(genericType, newTypeArguments);
    return this.apply(possibleNewParameterisedType);
  }

  private List<ISymbol> cloneWithEnclosingScope(final List<ISymbol> symbols, IScope enclosingScope) {
    return symbols.stream().map(symbol -> {
      var clonedSymbol = symbol.clone(enclosingScope);
      enclosingScope.define(clonedSymbol);
      return clonedSymbol;
    }).toList();
  }
}

package org.ek9lang.compiler.support;

import static org.ek9lang.compiler.support.CommonValues.SUBSTITUTED;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import org.ek9lang.compiler.ResolvedOrDefineResult;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.symbols.FunctionSymbol;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.IScope;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.symbols.PossibleGenericSymbol;
import org.ek9lang.compiler.tokenizer.IToken;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.CompilerException;

/**
 * Accepts an aggregate or a function that has been parameterized with 'type arguments'.
 * It goes through the 'generic type' with its 'type parameters' and the 'type arguments'
 * supplied and searches for the conceptual types 'T' etc. in what has been defined in the
 * 'generic type' and clones the method or parameter (in case of a function) and the associated
 * returns if there are any. It then replaces the 'T' with the appropriate 'type argument'.
 * This does mutate the 'possibleGenericSymbol' passed in.
 * <br/>
 * There is recursion and generics being replaced in here. VERY HARD.
 * This now only gets called for the FULL_RESOLUTION phase, prior to the Parameterized types are just
 * a placeholder. Empty of methods, but do have references to the type they are a parameterization of
 * and also dependent generic types.
 */
public class TypeSubstitution implements UnaryOperator<PossibleGenericSymbol> {

  private final ParameterizedSymbolCreator creator = new ParameterizedSymbolCreator(new InternalNameFor());

  /**
   * Deals with the conceptual mappings.
   */
  private final ConceptualLookupMapping conceptualLookupMapping = new ConceptualLookupMapping();

  private final CheckForDuplicateOperationsOnGeneric checkForDuplicateOperationsOnGeneric;

  /**
   * When cloning types if we encounter a cloned PossibleGenericSymbol must check if already held and use
   * that, or it must be defined so that it can be used through the application.
   */
  private final Function<PossibleGenericSymbol, ResolvedOrDefineResult> resolveOrDefine;

  /**
   * Construct a type substitutor.
   */
  public TypeSubstitution(final Function<PossibleGenericSymbol, ResolvedOrDefineResult> resolveOrDefine,
                          final ErrorListener errorListener) {

    AssertValue.checkNotNull("resolveOrDefine cannot be null", resolveOrDefine);
    this.checkForDuplicateOperationsOnGeneric = new CheckForDuplicateOperationsOnGeneric(errorListener);
    this.resolveOrDefine = resolveOrDefine;

  }

  @Override
  public PossibleGenericSymbol apply(final PossibleGenericSymbol parameterisedSymbol) {

    //mainly just preconditions
    AssertValue.checkNotNull("parameterisedSymbol cannot be null", parameterisedSymbol);
    AssertValue.checkFalse("parameterisedSymbol does not have any type arguments",
        parameterisedSymbol.getTypeParameterOrArguments().isEmpty());

    final var theGenericType = parameterisedSymbol.getGenericType();

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

    //This is the bit that either creates a new parameterised type or returns once previously created.
    final var result = resolveOrDefine.apply(parameterisedSymbol);
    final var resultingType = result.symbol();

    AssertValue.checkTrue("Result must be present", resultingType.isPresent());
    final var rtnType = resultingType.get();
    final var alreadySubstituted = "TRUE".equals(rtnType.getSquirrelledData(SUBSTITUTED));

    //Now mark it early as substituted - because this is recursive and will come back into this function!
    if (!alreadySubstituted) {
      rtnType.putSquirrelledData(SUBSTITUTED, "TRUE");
    }

    if (alreadySubstituted) {
      return rtnType;
    }

    //I never see this message - maybe I need some more tests to explore this.
    if (genericSymbol.isParameterisedType() && !"TRUE".equals(genericSymbol.getSquirrelledData(SUBSTITUTED))) {
      throw new CompilerException("Now you have a test that shows that even the generic type needs to be substituted");
    }

    //Now get the mapping of 'T' or whatever to actual parameters.
    final var typeMapping = conceptualLookupMapping.apply(rtnType.getTypeParameterOrArguments(),
        genericSymbol.getAnyConceptualTypeParameters());

    //Need to pass in the initial trigger for the generation of this parameterized type, as that will be the
    //same trigger for any dependent types.
    final var triggeredByToken = rtnType.getInitialisedBy();

    processAnyReferences(triggeredByToken, genericSymbol, typeMapping);

    //Otherwise do the business of changing the types.
    final var symbolsToClone = genericSymbol.getSymbolsForThisScope();
    final var clonedSymbols = cloneWithEnclosingScope(symbolsToClone, rtnType);

    replaceTypeParametersWithTypeArguments(triggeredByToken, rtnType, typeMapping, clonedSymbols);
    clonedSymbols.forEach(rtnType::define);

    if (genericSymbol instanceof FunctionSymbol genericFunctionSymbol
        && parameterisedSymbol instanceof FunctionSymbol functionSymbol
        && genericFunctionSymbol.isReturningSymbolPresent()) {

      final var clonedSymbol = genericFunctionSymbol.getReturningSymbol().clone(genericFunctionSymbol);

      substituteAsAppropriate(triggeredByToken, typeMapping, clonedSymbol);
      functionSymbol.setReturningSymbol(clonedSymbol);
    }

    if (rtnType instanceof IAggregateSymbol aggregate) {
      checkForDuplicateOperationsOnGeneric.accept(rtnType.getSourceToken(), aggregate);
    }

    return rtnType;
  }

  private void processAnyReferences(final IToken triggeredByToken,
                                    final PossibleGenericSymbol genericSymbol,
                                    final Map<ISymbol, ISymbol> typeMapping) {

    if (!genericSymbol.getGenericSymbolReferences().isEmpty()) {
      for (var genericSymbolReference : genericSymbol.getGenericSymbolReferences()) {

        final var newType = getReplacementType(triggeredByToken, typeMapping, genericSymbolReference);

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
  private void replaceTypeParametersWithTypeArguments(final IToken triggeredByToken,
                                                      final PossibleGenericSymbol possibleGenericSymbol,
                                                      final Map<ISymbol, ISymbol> typeMapping, List<ISymbol> symbols) {
    /*
    TODO ready for debug and fix of generics.
    if("Optional of type T of type Integer".equals(possibleGenericSymbol.getFriendlyName())) {
      System.out.printf("Substituting on %s%n", possibleGenericSymbol.getFriendlyName());
    }
     */

    symbols.forEach(symbol -> {
      if (symbol instanceof MethodSymbol methodSymbol) {
        if (methodSymbol.isConstructor()) {
          //Alter the name of the Method to match the name of the scope (i.e. class)
          //This is similar to Java where the name of the aggregate indicates a constructor.
          methodSymbol.setName(possibleGenericSymbol.getName());
          //Also need to alter the return type as a constructor.
          methodSymbol.setType(possibleGenericSymbol);
        }
        methodSymbol.getCallParameters().forEach(
            param -> substituteAsAppropriate(triggeredByToken, typeMapping, param)
        );
        if (methodSymbol.isReturningSymbolPresent()) {
          var returningSymbol = methodSymbol.getReturningSymbol();
          /*
          System.out.printf("In  substituting return on method [%s] [%s]%n",
              methodSymbol.getFriendlyName(),
              returningSymbol.getFriendlyName());
          */

          substituteAsAppropriate(triggeredByToken, typeMapping, returningSymbol);
          /*
          System.out.printf("Now substituting return on method [%s] [%s]%n",
              methodSymbol.getFriendlyName(),
              returningSymbol.getFriendlyName());
           */
        }
      }
      substituteAsAppropriate(triggeredByToken, typeMapping, symbol);
    });
  }

  private void substituteAsAppropriate(final IToken triggeredByToken,
                                       final Map<ISymbol, ISymbol> typeMapping,
                                       final ISymbol value) {

    final var paramType = value.getType();

    paramType.ifPresent(type -> value.setType(getReplacementTypeAsAppropriate(triggeredByToken, typeMapping, type)));

  }

  private ISymbol getReplacementTypeAsAppropriate(final IToken triggeredByToken,
                                                  final Map<ISymbol, ISymbol> typeMapping, ISymbol forType) {

    //Have to be aware some types might be generic, others are just simple.
    if (forType instanceof PossibleGenericSymbol couldBeGeneric && couldBeGeneric.isGenericInNature()) {
      return getReplacementType(triggeredByToken, typeMapping, couldBeGeneric);
    } else if (typeMapping.containsKey(forType)) {
      return typeMapping.get(forType);
    }

    return forType;
  }

  private ISymbol getReplacementType(final IToken triggeredByToken,
                                     final Map<ISymbol, ISymbol> typeMapping,
                                     final PossibleGenericSymbol forType) {

    //This is a scenario where we have 'D1 of type X of type P' i.e. conceptual.
    //So if we are now to replace the 'P' with something we need to go all the way down!
    //because this could be a 'D1 of type (X, Y, Z) of type (P, Date, G2 of (P, P))

    final var theGenericType = forType.getGenericType();
    AssertValue.checkTrue("Must have a generic type for [" + forType.getFriendlyName() + "]",
        theGenericType.isPresent());

    final var genericType = theGenericType.get();
    final var newTypeArguments = new ArrayList<ISymbol>();

    for (var typeArgument : forType.getTypeParameterOrArguments()) {
      final var replacementType = getReplacementTypeAsAppropriate(triggeredByToken, typeMapping, typeArgument);

      newTypeArguments.add(replacementType);
    }

    //OK, so now we may have a new set of type arguments so as we are in 'generic world' we need to resolve or define.
    //So first create the outline - this is just an outline. If the type already exists then it will be returned
    //Otherwise this outline will be used - the fleshed out later.

    final var possibleNewParameterisedType = creator.apply(genericType, newTypeArguments);

    possibleNewParameterisedType.setInitialisedBy(triggeredByToken);

    return this.apply(possibleNewParameterisedType);
  }

  /**
   * Does not define the new symbol in the enclosing scope, because constructors must have their name altered
   * Only then can the be added to the enclosing scope via 'define.
   */
  private List<ISymbol> cloneWithEnclosingScope(final List<ISymbol> symbols, final IScope enclosingScope) {

    return symbols.stream().map(symbol -> symbol.clone(enclosingScope)).toList();

  }
}

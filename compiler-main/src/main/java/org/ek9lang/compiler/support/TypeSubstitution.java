package org.ek9lang.compiler.support;

import static org.ek9lang.compiler.support.CommonValues.SUBSTITUTED;

import java.util.ArrayList;
import java.util.List;
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
 * Note that there are examples of interdependent generic types in ek9 source, these actually
 * refer to each other and in such scenarios it is inevitable that Generic of type (X, Y) ends up
 * through references being parameterized with the same 'X' and 'Y'. Resulting in (well) 'Generic of (X, Y),
 * i.e. not really being parameterized at all. In such situations we already have the generic type.
 * When List of T, Iterator of T are set up and created - as they are the first encountered the 'T'
 * is the same. But Optional of T (that's a different T) when it triggers Iterator of T - the T is
 * from the List of T. So 'T' is not just a common 'T' it is a 'T' that is scoped and defined within a generic Type.
 * This is a fundamental issue, really it is the fact that T is an unconstrained type that is the important bit.
 * So there error is probably not in here, but in the way I'm defining the Generic type parameters (like T).
 * Where it is K or V or T if it is an unconstrained type then the T is the variable and the type is an aspect of it.
 * However, it could be in some circumstances a T is constrained to be An Integer or some other type.
 * So these constraints also need modelling as a type.
 */
public class TypeSubstitution implements UnaryOperator<PossibleGenericSymbol> {

  private final ParameterizedSymbolCreator creator
      = new ParameterizedSymbolCreator(new InternalNameFor());

  private final ValidPossibleGenericSymbolOrException validPossibleGenericSymbolOrError
      = new ValidPossibleGenericSymbolOrException();

  /**
   * Deals with the conceptual mappings.
   */
  private final PositionalConceptualLookupMapping conceptualLookupMapping = new PositionalConceptualLookupMapping();

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

    validPossibleGenericSymbolOrError.accept(parameterisedSymbol);

    final var theGenericType = parameterisedSymbol.getGenericType();

    //OK now on to the processing.
    AssertValue.checkTrue("Generic Type for Parameterization must be present", theGenericType.isPresent());
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

    //Now you may not expect it, but it's possible/probable that a generic type when parameterised with
    //arguments that fully match its own type parameters will result in just the same generic type being provided.
    //For example List of type T when parameterised with its own 'T' is still just a 'List of T'.

    if (rtnType == genericSymbol) {
      //There is no need to substitute anything as this is still just a generic class as passed in.
      return rtnType;
    }

    final var alreadySubstituted = "TRUE".equals(rtnType.getSquirrelledData(SUBSTITUTED));

    //Now mark it early as substituted - because this is indirectly recursive and will come back into this function!
    //This recursion is not immediately obvious. But there is a flow, that looks for types that need to be created.
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

    final var parameterizedWithArguments = parameterisedSymbol.getTypeParameterOrArguments();

    processAnyReferences(triggeredByToken, genericSymbol, parameterizedWithArguments);

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
                                    final List<ISymbol> parameterizingArguments) {

    final var typeMapping = conceptualLookupMapping.apply(parameterizingArguments,
        genericSymbol.getAnyConceptualTypeParameters());

    if (!genericSymbol.getGenericSymbolReferences().isEmpty()) {
      for (var genericSymbolReference : genericSymbol.getGenericSymbolReferences()) {

        final var possibleNewType = getReplacementType(triggeredByToken, typeMapping, genericSymbolReference);
        AssertValue.checkNotNull("Expecting new type to be created", possibleNewType);
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
                                                      final PositionalConceptualLookupMapping.Mapping typeMapping,
                                                      List<ISymbol> symbols) {

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
          substituteAsAppropriate(triggeredByToken, typeMapping, returningSymbol);
        }
      }
      substituteAsAppropriate(triggeredByToken, typeMapping, symbol);

    });
  }

  private void substituteAsAppropriate(final IToken triggeredByToken,
                                       final PositionalConceptualLookupMapping.Mapping typeMapping,
                                       final ISymbol value) {

    final var paramType = value.getType();

    paramType.ifPresent(type -> value.setType(getReplacementTypeAsAppropriate(triggeredByToken, typeMapping, type)));

  }

  private ISymbol getReplacementTypeAsAppropriate(final IToken triggeredByToken,
                                                  final PositionalConceptualLookupMapping.Mapping typeMapping,
                                                  ISymbol forType) {


    //Have to be aware some types might be generic, others are just simple.
    if (forType instanceof PossibleGenericSymbol couldBeGeneric && couldBeGeneric.isGenericInNature()) {
      return getReplacementType(triggeredByToken, typeMapping, couldBeGeneric);
    } else {
      for (int i = 0; i < typeMapping.typeParameters().size(); i++) {
        final var conceptualParameter = typeMapping.typeParameters().get(i);
        if (conceptualParameter.isExactSameType(forType)) {
          return typeMapping.typeArguments().get(i);
        }
      }
    }

    return forType;
  }

  private ISymbol getReplacementType(final IToken triggeredByToken,
                                     final PositionalConceptualLookupMapping.Mapping typeMapping,
                                     final PossibleGenericSymbol forType) {

    //This is a scenario where we have 'D1 of type X of type P' i.e. conceptual.
    //So if we are now to replace the 'P' with something we need to go all the way down!
    //because this could be a 'D1 of type (X, Y, Z) of type (P, Date, G2 of (P, P))

    //But we may also just be in a simple case of forType being the generic type!
    //In addition, it is possible the type has been parameterised with its own parameters again!
    //So thats not really a new type.

    final var genericType = forType.getGenericType().isPresent() ? forType.getGenericType().get() : forType;
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
   * Only then can there
   * .be added to the enclosing scope via 'define.
   */
  private List<ISymbol> cloneWithEnclosingScope(final List<ISymbol> symbols, final IScope enclosingScope) {

    return symbols.stream().map(symbol -> symbol.clone(enclosingScope)).toList();

  }
}

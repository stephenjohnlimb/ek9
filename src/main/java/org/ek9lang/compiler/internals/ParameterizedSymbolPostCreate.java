package org.ek9lang.compiler.internals;

import java.util.function.Function;
import java.util.function.UnaryOperator;
import org.ek9lang.compiler.symbol.PossibleGenericSymbol;
import org.ek9lang.compiler.symbol.support.ConceptualFlatteningMapping;
import org.ek9lang.compiler.symbol.support.ParameterizedGenericSymbolCreator;
import org.ek9lang.compiler.symbol.support.TypeSubstitution;
import org.ek9lang.core.exception.AssertValue;

/**
 * Struggling to find a better name than this.
 * When a 'parameterized type' - which consists of a ('generic type' - with 'type parameters') and 'type arguments'.
 * is supplied, this function checks with the resolverOrDefiner to either resolve or define the new type.
 * IF: a new type is defined, then and ONLY then must this function:
 * 1. Check if there are dependent generic types that must now be processed - these are the types depend on the
 * 'T'.
 * 2. Populate all the methods on the new definition (replacing the 'T' with the appropriate 'type argument'.
 * Hence, this is a sort of recursive 'cascade'.
 * If 'List of type T' has dependents of 'Iterator of type T', 'Map of type (Integer, T)'
 * And 'Map of type (Integer, T)' has dependents of 'List of type T'.
 * You can see you'd end up in a never ending loop!
 * SO, in the example of where 'T' -> 'String', first define the name for 'List of type String'.
 * Now that means that while that type has been defined - it is still only a 'name', there are no methods or anything.
 * Now go through the 'dependent type references' , i.e. the 'Iterator of type String'
 * and the 'Map of type (Integer, String)'. Calling for each JUST to be defined.
 * Now when we get to 'Map of type (Integer, String)', recall it has dependent of 'List of type String'!!!
 * So this is why we have the resolverOrDefiner (it has the memory), because we 'recorded' the name of
 * 'List of type String' right at the very start, the cascade comes to an end (at least for this path).
 * Now all the methods can be populated on those defined types. This is because all dependent types being used on those
 * methods have previously been recorded as dependent type references.
 * This is also complicated by the fact it is also necessary to substitute parameters through multiple layers.
 */
public class ParameterizedSymbolPostCreate implements UnaryOperator<PossibleGenericSymbol> {

  private final Function<PossibleGenericSymbol, ResolvedOrDefineResult> resolverOrDefiner;

  private final ParameterizedGenericSymbolCreator creator = new ParameterizedGenericSymbolCreator();

  private final ConceptualFlatteningMapping conceptualFlatteningMapping = new ConceptualFlatteningMapping();

  private final TypeSubstitution typeSubstitution = new TypeSubstitution();

  public ParameterizedSymbolPostCreate(
      final Function<PossibleGenericSymbol, ResolvedOrDefineResult> resolverOrDefiner) {
    this.resolverOrDefiner = resolverOrDefiner;
  }

  /**
   * Firstly resolves or defines the new parameterized type.
   * If resolved - that's it we're done.
   * If newly defined - well that's a different story. The code now
   * has to define any referenced generic types that depended on one or more of the
   * 'generic types' 'type parameters'.
   */
  @Override
  public PossibleGenericSymbol apply(PossibleGenericSymbol possibleGenericSymbol) {
    var result = resolverOrDefiner.apply(possibleGenericSymbol);
    var rtn = getReturnSymbol(result);
    if (result.newlyDefined()) {
      //Trigger the cascade, which will be recursive, but with different types, until not newly defined!
      createParameterizedGenericSymbolsFromReferences(possibleGenericSymbol);

      //Now on the tail end - once all types created - populate the methods (as all types can now be resolved).
      typeSubstitution.apply(rtn);
    }
    return rtn;
  }

  /**
   * This is a bit tricky, because we have dependent 'generic types' with their 'type parameters' and a number
   * of 'type arguments' in this parameterizedGenericSymbol. But the types in the 'generic type' - 'type parameters'
   * could be concrete and not need replacing.
   * So within the dependent type the 'T' or 'K' or whatever will have been resolved in the parent generic scope.
   */
  private void createParameterizedGenericSymbolsFromReferences(final PossibleGenericSymbol parameterizedGenericSymbol) {
    var theGenericType = parameterizedGenericSymbol.getGenericType();
    AssertValue.checkTrue("Expecting generic type to be present", theGenericType.isPresent());
    theGenericType.ifPresent(genericType -> {
      //OK so we have a generic type, lets see if that has any references to be processed.
      if (!genericType.getGenericSymbolReferences().isEmpty()) {

        //If it does then the types need to be substituted.
        var newFlattenedTypes = genericType
            .getGenericSymbolReferences()
            .stream()
            .map(dependentType -> substituteByFlatteningTypes(parameterizedGenericSymbol, dependentType))
            .toList();

        //Once created as a new parameterized type with the new type arguments, it must be resolved or defined.
        //Now the recursion or not if already defined. So if we encounter a type already created the recursion will stop
        //The stack will unwind.
        newFlattenedTypes.forEach(this::apply);
      }
    });
  }

  /**
   * More tricky than you'd expect, Because you've got three layers of parameterised types here.
   * <pre>
   *   1. The top level outer parameterised type with its set of final 'type arguments'
   *   2. The immediate dependent parameterised type and its set of 'type arguments'.
   *   3. Finally the 'generic type' from the dependent type and its set of 'type parameters'
   * </pre>
   * What we want to do is create yet another parameterised type from (3), but use the 'appropriate'
   * 'type arguments' built up through the parameterised types (3) -> (2) -> (1).
   * This is more difficult than you'd think, because some could be concrete already and not need replacing.
   */
  private PossibleGenericSymbol substituteByFlatteningTypes(final PossibleGenericSymbol parameterizedGenericSymbol,
                                                            final PossibleGenericSymbol dependentType) {
    //So this is the final set to be used in the dependent type flattening
    var finalSetOfTypeArguments = parameterizedGenericSymbol.getTypeParameterOrArguments();

    //We must now pull up through the dependentType via its 'generic type' + 'type parameters' and 'type arguments'.
    var dependentGenericType = dependentType.getGenericType();
    AssertValue.checkTrue("Compiler error expecting dependent type to have a generic type",
        dependentGenericType.isPresent());

    var typeArguments = dependentType.getTypeParameterOrArguments();
    var typeParameters = dependentGenericType.get().getTypeParameterOrArguments();

    var baseMappedParameters = conceptualFlatteningMapping.apply(typeArguments, typeParameters);

    //Now we use that in conjunction with the final set of type arguments.
    var finalSetOfTypes = conceptualFlatteningMapping.apply(finalSetOfTypeArguments, baseMappedParameters);

    return creator.apply(dependentGenericType.get(), finalSetOfTypes);
  }

  private PossibleGenericSymbol getReturnSymbol(final ResolvedOrDefineResult result) {
    PossibleGenericSymbol rtn = null;
    var symbol = result.symbol();
    AssertValue.checkTrue("Compiler Error, failed to either create or resolve a parameterized type",
        symbol.isPresent());

    if (symbol.get() instanceof PossibleGenericSymbol returnSymbol) {
      rtn = returnSymbol;
    }
    return rtn;
  }
}

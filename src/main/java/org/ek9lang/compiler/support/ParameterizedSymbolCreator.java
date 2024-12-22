package org.ek9lang.compiler.support;

import java.util.List;
import java.util.function.BiFunction;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.FunctionSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.PossibleGenericSymbol;
import org.ek9lang.compiler.symbols.SymbolCategory;
import org.ek9lang.core.AssertValue;

/**
 * This will just create a Aggregate/Function 'parameterized type' of the 'generic type' with the 'type arguments'
 * But note it will have not - repeat NOT have - traversed the 'generic type' and cloned over the methods and
 * altered all the 'T', 'K', 'V' - 'type parameters' for each of the 'type arguments'
 * There is a separate function (TypeSubstitution) that does that.
 * This is because the timing of when to do that is important.
 */
public class ParameterizedSymbolCreator
    implements BiFunction<PossibleGenericSymbol, List<ISymbol>, PossibleGenericSymbol> {

  private final ConceptualLookupMapping conceptualLookupMapping = new ConceptualLookupMapping();

  private final BiFunction<PossibleGenericSymbol, List<ISymbol>, String> internalNameFor;


  public ParameterizedSymbolCreator(final BiFunction<PossibleGenericSymbol, List<ISymbol>, String> internalNameFor) {
    this.internalNameFor = internalNameFor;

  }

  //Not smart enough to work out null pointer cannot happen.
  @SuppressWarnings("java:S2259")
  @Override
  public PossibleGenericSymbol apply(final PossibleGenericSymbol possibleGenericSymbol,
                                     final List<ISymbol> typeArguments) {

    AssertValue.checkNotNull("possibleGenericSymbol cannot be null", possibleGenericSymbol);
    AssertValue.checkNotNull("typeArguments cannot be null", typeArguments);
    AssertValue.checkNotEmpty("typeArguments cannot be empty", typeArguments);
    AssertValue.checkTrue("possibleGenericSymbol must be generic in nature", possibleGenericSymbol.isGenericInNature());
    AssertValue.checkTrue("type parameters and type arguments must be same size "
            + possibleGenericSymbol.getAnyConceptualTypeParameters().size() + " and " + typeArguments.size(),
        possibleGenericSymbol.getAnyConceptualTypeParameters().size() == typeArguments.size());

    PossibleGenericSymbol rtn = null;

    if (possibleGenericSymbol.getCategory().equals(SymbolCategory.TEMPLATE_TYPE)) {
      //Going to create a TYPE or another TEMPLATE_TYPE
      rtn = createAggregate(possibleGenericSymbol, typeArguments);
    } else if (possibleGenericSymbol.getCategory().equals(SymbolCategory.TEMPLATE_FUNCTION)
        && possibleGenericSymbol instanceof FunctionSymbol asFunctionSymbol) {
      //Going to be a FUNCTION or another TEMPLATE_FUNCTION
      rtn = createFunction(asFunctionSymbol, typeArguments);
    }

    AssertValue.checkNotNull("PossibleGenericSymbol "
        + possibleGenericSymbol.getCategory()
        + " does not make sense, rtn is null", rtn);

    //Now clone over the abstract/extensibility nature from the generic type
    rtn.setOpenForExtension(possibleGenericSymbol.isOpenForExtension());
    rtn.setMarkedAbstract(possibleGenericSymbol.isMarkedAbstract());
    rtn.setMarkedPure(possibleGenericSymbol.isMarkedPure());

    return rtn;
  }

  private AggregateSymbol createAggregate(final PossibleGenericSymbol possibleGenericSymbol,
                                          final List<ISymbol> typeArguments) {

    //Get the decorated name.
    final var name = internalNameFor.apply(possibleGenericSymbol, typeArguments);
    final var rtn = new AggregateSymbol(name, possibleGenericSymbol.getModuleScope());

    rtn.setGenericType(possibleGenericSymbol);
    typeArguments.forEach(rtn::addTypeParameterOrArgument);

    return rtn;
  }

  private FunctionSymbol createFunction(final FunctionSymbol functionGenericSymbol,
                                        final List<ISymbol> typeArguments) {

    final var name = internalNameFor.apply(functionGenericSymbol, typeArguments);
    final var rtn = new FunctionSymbol(name, functionGenericSymbol.getModuleScope());

    rtn.setGenericType(functionGenericSymbol);
    typeArguments.forEach(rtn::addTypeParameterOrArgument);
    //As I missed this before - lets panic if this is not set as it means the result of the processing will
    //cause other errors that will be hard to track down.
    AssertValue.checkNotNull("Expecting a returning Symbol", functionGenericSymbol.getReturningSymbol());

    //What about the return type, need to deal with at as well.
    final var clonedReturnSymbol = functionGenericSymbol.getReturningSymbol().clone(rtn);
    //But we're not quite finished - because the type might be a 'T' and so we need to find 'T' in the generic type
    //get its position and look that up in the typeArguments.

    final var returnType = clonedReturnSymbol.getType();
    AssertValue.checkTrue("Expect returning type to be present", returnType.isPresent());

    if (returnType.get().isConceptualTypeParameter()) {
      var mapping = conceptualLookupMapping.apply(typeArguments, functionGenericSymbol.getTypeParameterOrArguments());
      var mappedType = mapping.get(returnType.get());
      //Now change the type of the cloned return symbol
      clonedReturnSymbol.setType(mappedType);
    }

    rtn.setReturningSymbol(clonedReturnSymbol);

    return rtn;
  }

}

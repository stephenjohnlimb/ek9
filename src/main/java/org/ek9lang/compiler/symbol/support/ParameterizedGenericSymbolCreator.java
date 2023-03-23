package org.ek9lang.compiler.symbol.support;

import java.util.List;
import java.util.function.BiFunction;
import org.ek9lang.compiler.symbol.AggregateSymbol;
import org.ek9lang.compiler.symbol.FunctionSymbol;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.PossibleGenericSymbol;
import org.ek9lang.core.exception.AssertValue;

/**
 * This will just create a Aggregate/Function 'parameterized type' of the 'generic type' with the 'type arguments'
 * But note it will have not - repeat not have - traversed the 'generic type' and cloned over the methods and
 * altered all the 'T', 'K', 'V' - 'type parameters' for each of the 'type arguments'
 * There is a separate function that does that. This is because the timing of when to do that is important.
 */
public class ParameterizedGenericSymbolCreator
    implements BiFunction<PossibleGenericSymbol, List<ISymbol>, PossibleGenericSymbol> {

  private final InternalNameFor internalNameFor = new InternalNameFor();

  @Override
  public PossibleGenericSymbol apply(final PossibleGenericSymbol possibleGenericSymbol,
                                     final List<ISymbol> typeArguments) {
    AssertValue.checkNotNull("possibleGenericSymbol cannot be null", possibleGenericSymbol);
    AssertValue.checkNotNull("typeArguments cannot be null", typeArguments);
    AssertValue.checkNotEmpty("typeArguments cannot be empty", typeArguments);
    AssertValue.checkTrue("possibleGenericSymbol must be generic in nature", possibleGenericSymbol.isGenericInNature());
    AssertValue.checkTrue("type parameters and type arguments must be same size",
        possibleGenericSymbol.getTypeParameterOrArguments().size() == typeArguments.size());

    PossibleGenericSymbol rtn = null;

    if (possibleGenericSymbol.getCategory().equals(ISymbol.SymbolCategory.TEMPLATE_TYPE)) {
      //Going to create a TYPE or another TEMPLATE_TYPE
      rtn = createAggregate(possibleGenericSymbol, typeArguments);
    } else if (possibleGenericSymbol.getCategory().equals(ISymbol.SymbolCategory.TEMPLATE_FUNCTION)) {
      //Going to be a FUNCTION or another TEMPLATE_FUNCTION
      rtn = createFunction(possibleGenericSymbol, typeArguments);
    }

    AssertValue.checkNotNull("PossibleGenericSymbol " + possibleGenericSymbol.getCategory() + " does not make sense",
        rtn);

    return rtn;
  }

  private AggregateSymbol createAggregate(final PossibleGenericSymbol possibleGenericSymbol,
                                          final List<ISymbol> typeArguments) {
    //Get the decorated name.
    var name = internalNameFor.apply(possibleGenericSymbol, typeArguments);
    var rtn = new AggregateSymbol(name, possibleGenericSymbol.getModuleScope());

    rtn.setGenericType(possibleGenericSymbol);
    typeArguments.forEach(rtn::addTypeParameterOrArgument);

    return rtn;
  }

  private FunctionSymbol createFunction(final PossibleGenericSymbol possibleGenericSymbol,
                                        final List<ISymbol> typeArguments) {
    var name = internalNameFor.apply(possibleGenericSymbol, typeArguments);
    var rtn = new FunctionSymbol(name, possibleGenericSymbol.getModuleScope());

    rtn.setGenericType(possibleGenericSymbol);
    typeArguments.forEach(rtn::addTypeParameterOrArgument);

    return rtn;
  }
}

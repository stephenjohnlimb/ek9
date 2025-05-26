package org.ek9lang.compiler.support;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.core.AssertValue;

/**
 * You may think why this mapping.
 * It's because some 'type parameters' in the generic type may be 'concrete' and not 'conceptual'.
 * i.e. 'type arguments' = [Integer, String, Duration] but
 * the  'type parameters' = [Float, X, Y, Boolean, Z] - this might be the in a generic type that also has
 * a generic type and has been parameterized with a mix of conceptual and concrete arguments.
 * This mapping works on 'positions' in the order of both defined generic parameters and the arguments passed in during
 * parametrisation (to concrete or other conceptual types).
 */
public class PositionalConceptualLookupMapping
    implements BiFunction<List<ISymbol>, List<ISymbol>, PositionalConceptualLookupMapping.Mapping> {
  @Override
  public Mapping apply(final List<ISymbol> typeArguments, final List<ISymbol> typeParameters) {

    AssertValue.checkNotNull("TypeParameters cannot null", typeParameters);
    AssertValue.checkNotNull("TypeArguments cannot null", typeArguments);
    AssertValue.checkFalse("TypeArguments must be greater than zero", typeArguments.isEmpty());

    final List<ISymbol> rtn = new ArrayList<>();

    int index = 0;
    for (ISymbol typeParameter : typeParameters) {
      if (typeParameter.isConceptualTypeParameter()) {
        rtn.add(typeParameter);
        index++;
      }
    }

    //Post condition check, to help when I mess the compiler up!
    AssertValue.checkTrue("Number of conceptual types to supplied types must match",
        index == typeArguments.size());

    return new Mapping(typeArguments, rtn);
  }

  /**
   * Just holds a simple set of lists with the appropriate type arguments and their corresponding parameterizing
   * parameters. This just enables a single index based lookup based on positions in Lists.
   */
  public record Mapping(List<ISymbol> typeArguments, List<ISymbol> typeParameters) {

  }
}

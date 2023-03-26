package org.ek9lang.compiler.symbol.support;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.core.exception.AssertValue;

/**
 * You may think why this mapping.
 * It's because some 'type parameters' in the generic type may be 'concrete' and not 'conceptual'.
 * i.e. 'type arguments' = [Integer, String, Duration] but
 * the  'type parameters' = [Float, X, Y, Boolean, Z] - this might be the in a generic type that also has
 * a generic type and has been parameterized with a mix of conceptual and concrete arguments.
 */
public class ConceptualLookupMapping implements BiFunction<List<ISymbol>, List<ISymbol>, Map<ISymbol, ISymbol>> {
  @Override
  public Map<ISymbol, ISymbol> apply(final List<ISymbol> typeArguments, final List<ISymbol> typeParameters) {
    Map<ISymbol, ISymbol> rtn = new HashMap<>();
    int index = 0;
    for (ISymbol typeParameter : typeParameters) {
      if (typeParameter.isConceptualTypeParameter()) {
        var typeArgument = typeArguments.get(index);
        rtn.put(typeParameter, typeArgument);
        index++;
      }
    }
    AssertValue.checkTrue("Number of conceptual types to supplied types must match",
        index == typeArguments.size());
    return rtn;
  }
}

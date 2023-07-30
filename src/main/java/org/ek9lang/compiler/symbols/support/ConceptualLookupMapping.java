package org.ek9lang.compiler.symbols.support;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.core.AssertValue;

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
    AssertValue.checkNotNull("TypeParameters cannot null", typeParameters);
    AssertValue.checkNotNull("TypeArguments cannot null", typeArguments);
    AssertValue.checkFalse("TypeArguments must be greater than zero", typeArguments.isEmpty());
    Map<ISymbol, ISymbol> rtn = new HashMap<>();
    int index = 0;
    for (ISymbol typeParameter : typeParameters) {
      if (typeParameter.isConceptualTypeParameter()) {
        var typeArgument = typeArguments.get(index);
        rtn.put(typeParameter, typeArgument);
        index++;
      }
    }
    //Post condition check, to help when I mess the compiler up!
    AssertValue.checkTrue("Number of conceptual types to supplied types must match",
        index == typeArguments.size());
    return rtn;
  }
}

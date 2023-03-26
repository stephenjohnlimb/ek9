package org.ek9lang.compiler.symbol.support;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BinaryOperator;
import org.ek9lang.compiler.symbol.ISymbol;

/**
 * Again some 'type parameters' in the generic type may be 'concrete' and not 'conceptual'.
 * i.e. 'type arguments' = [Integer, String, Duration] but
 * the  'type parameters' = [Float, X, Y, Boolean, Z] - this might be the in a generic type that also has
 * a generic type and has been parameterized with a mix of conceptual and concrete arguments.
 * But rather than produce a mapping of just the X, Y and Z to Integer, Float and Duration,
 * This 'flattening' mapping will produce [Float, Integer, Float, Boolean, Duration], in other words, when
 * there is no conceptual mapping this mapper will 'pull through the existing concrete type'.
 */
public class ConceptualFlatteningMapping implements BinaryOperator<List<ISymbol>> {
  @Override
  public List<ISymbol> apply(final List<ISymbol> typeArguments, final List<ISymbol> typeParameters) {
    List<ISymbol> rtn = new ArrayList<>();
    int index = 0;
    for (ISymbol typeParameter : typeParameters) {
      if (typeParameter.isConceptualTypeParameter()) {
        var typeArgument = typeArguments.get(index);
        rtn.add(typeArgument);
        index++;
      } else {
        //So it's a concrete type - we just pass that straight through.
        rtn.add(typeParameter);
      }
    }
    return rtn;
  }
}

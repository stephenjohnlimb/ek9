package org.ek9lang.compiler.symbols.generics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.ek9lang.compiler.support.ConceptualLookupMapping;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.base.AbstractSymbolTestBase;
import org.junit.jupiter.api.Test;

/**
 * The multiple mappings of real and conceptual types when working on generics means
 * this test is essential to break the problem down and try lots of possible combinations.
 * This test on checks 'ConceptualLookupMapping' and that is only used once 'TypeSubstitution'.
 * But the concept is so confusing/complex (for me anyway), I've pulled it out for separate coding and testing.
 * <pre>
 *   (R, S) : (Float, String)
 *   (K, V) : (Integer, S)
 * </pre>
 * But also things like this - where a partially parameterised type has the remaining conceptual types provided.
 * In definition, it had:
 * <pre>
 *   (X, Y, S, Z, R, A) : then X->Duration, Y->Boolean, S (left as-is), Z->Time, R (left as-is), A->Float.
 * </pre>
 * Resulting in this below - so now when we add (String, Float) we're expecting S->String and R->Float
 * <pre>
 *   (Duration, Boolean, S, Time, R, Float) : (String, Float)
 *   (Duration, Boolean, String, Time, Float, Float)
 * </pre>
 * But if there had been a dependent type that had been defined in terms of:
 * <pre>
 *   (X, A, R, S) : Then it would have first resulted in (Duration, Float, R, S) and then when finally parameterised
 *   (Duration, Float, Float, String)
 * </pre>
 * 'TypeSubstitution' actually uses recursion to accomplish the same sort of functionality.
 */
class GenericArgumentAndParameterTest extends AbstractSymbolTestBase {

  private final ConceptualLookupMapping conceptualLookupMapping = new ConceptualLookupMapping();

  @Test
  void testSimpleConceptualMapping() {
    //(R, S) : (Float, String)
    var r = support.createGenericT("R", symbolTable);
    var s = support.createGenericT("S", symbolTable);

    //These are the parameters on the generic type
    List<ISymbol> typeParameters = List.of(r, s);
    //These are the type arguments passed in when parameterizing.
    List<ISymbol> typeArguments = List.of(ek9Float, ek9String);

    var mapping = conceptualLookupMapping.apply(typeArguments, typeParameters);
    assertEquals(ek9Float, mapping.get(r));
    assertEquals(ek9String, mapping.get(s));
  }

  @Test
  void testMixedConcreteAndConceptualMapping() {
    //(Boolean, R, Duration, S, Integer) : (Float, String)
    var r = support.createGenericT("R", symbolTable);
    var s = support.createGenericT("S", symbolTable);

    //So this models a sort of partially parameterized type, with the additional concrete types now being provided.
    List<ISymbol> typeParameters = List.of(ek9Boolean, r, ek9Duration, s, ek9Integer);
    List<ISymbol> typeArguments = List.of(ek9Float, ek9String);

    var mapping = conceptualLookupMapping.apply(typeArguments, typeParameters);
    assertEquals(ek9Float, mapping.get(r));
    assertEquals(ek9String, mapping.get(s));
  }

  @Test
  void testConceptualToConceptualMapping() {
    //(Boolean, R, Duration, S, Integer) : (Float, T)
    var r = support.createGenericT("R", symbolTable);
    var s = support.createGenericT("S", symbolTable);
    var t = support.createGenericT("T", symbolTable);

    //While not really any different - here the mapping would result in a parameterized type
    //that would need to be parameterized further before being useful.
    List<ISymbol> typeParameters = List.of(ek9Boolean, r, ek9Duration, s, ek9Integer);
    List<ISymbol> typeArguments = List.of(ek9Float, t);

    var mapping = conceptualLookupMapping.apply(typeArguments, typeParameters);
    assertEquals(ek9Float, mapping.get(r));
    assertEquals(t, mapping.get(s));
  }
}

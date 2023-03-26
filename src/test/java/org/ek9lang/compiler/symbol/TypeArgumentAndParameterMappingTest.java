package org.ek9lang.compiler.symbol;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.ek9lang.compiler.symbol.support.ConceptualLookupMapping;
import org.ek9lang.compiler.symbol.support.ConceptualFlatteningMapping;
import org.junit.jupiter.api.Test;

/**
 * The multiple mappings of real and conceptual types when working on generics means
 * this test is essential to break the problem down and try lots of possible combinations.
 * <pre>
 *   (R, S) : (Float, String)
 *   (K, V) : (Integer, S)
 * </pre>
 * But also things like this - where a partially parameterised type has the remaining conceptual types provided.
 * In definition, it had:
 * <pre>
 *   (X, Y, S, Z, R, A) : then X->Duration, Y->Boolean, S (left), Z->Time, R (left), A->Float.
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
 */
class TypeArgumentAndParameterMappingTest extends AbstractSymbolTestBase {

  private final ConceptualLookupMapping conceptualLookupMapping = new ConceptualLookupMapping();

  private final ConceptualFlatteningMapping conceptualFlatteningMapping = new ConceptualFlatteningMapping();

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

  @Test
  void testSimpleFlatteningMapping() {
    //(R, S) : (Float, String)
    var r = support.createGenericT("R", symbolTable);
    var s = support.createGenericT("S", symbolTable);

    List<ISymbol> typeParameters = List.of(r, s);
    List<ISymbol> typeArguments = List.of(ek9Float, ek9String);

    //The result should be the same as the typeArguments.
    var mapped = conceptualFlatteningMapping.apply(typeArguments, typeParameters);
    assertEquals(typeArguments, mapped);
  }

  @Test
  void testMixedFlatteningConcreteAndConceptualMapping() {
    //(Boolean, R, Duration, S, Integer) : (Float, String)
    var r = support.createGenericT("R", symbolTable);
    var s = support.createGenericT("S", symbolTable);

    //So this models a sort of partially parameterized type, with the additional concrete types now being provided.
    List<ISymbol> typeParameters = List.of(ek9Boolean, r, ek9Duration, s, ek9Integer);
    List<ISymbol> typeArguments = List.of(ek9Float, ek9String);
    var mapped = conceptualFlatteningMapping.apply(typeArguments, typeParameters);
    var expectation = List.of(ek9Boolean, ek9Float, ek9Duration, ek9String, ek9Integer);

    assertEquals(expectation, mapped);
  }

  @Test
  void testFlatteningConceptualToConceptualMapping() {
    //(Boolean, R, Duration, S, Integer) : (Float, T)
    var r = support.createGenericT("R", symbolTable);
    var s = support.createGenericT("S", symbolTable);
    var t = support.createGenericT("T", symbolTable);

    //While not really any different - here the mapping would result in a parameterized type
    //that would need to be parameterized further before being useful.
    List<ISymbol> typeParameters = List.of(ek9Boolean, r, ek9Duration, s, ek9Integer);
    List<ISymbol> typeArguments = List.of(ek9Float, t);

    var mapped = conceptualFlatteningMapping.apply(typeArguments, typeParameters);
    var expectation = List.of(ek9Boolean, ek9Float, ek9Duration, t, ek9Integer);

    assertEquals(expectation, mapped);
  }

  /**
   * Given definition:
   * <pre>
   *   SomeGeneric of type (A, B, C, D, E)
   * </pre>
   * Then a partial Parameterization of:
   * <pre>
   *   SomeGeneric of type (A, B, C, D, E) of type (Integer, R, Boolean, S, Float)
   * </pre>
   * And then another Parameterization of:
   * <pre>
   *   SomeGeneric of type (A, B, C, D, E) of type (Integer, R, Boolean, S, Float) of type (Float, Time)
   * </pre>
   * We would expect the final concrete parameterization to be:
   * <pre>
   *   SomeGeneric of (Integer, Float, Boolean Time, Float)
   * </pre>
   */
  @Test
  void testMultiFlatteningMapping() {
    var a = support.createGenericT("A", symbolTable);
    var b = support.createGenericT("B", symbolTable);
    var c = support.createGenericT("C", symbolTable);
    var d = support.createGenericT("D", symbolTable);
    var e = support.createGenericT("E", symbolTable);

    var r = support.createGenericT("R", symbolTable);
    var s = support.createGenericT("S", symbolTable);

    List<ISymbol> typeParametersBase = List.of(a, b, c, d, e);
    List<ISymbol> typeArgumentsBase = List.of(ek9Integer, r, ek9Boolean, s, ek9Float);

    var mappedBase = conceptualFlatteningMapping.apply(typeArgumentsBase, typeParametersBase);
    var expectedBase = List.of(ek9Integer, r, ek9Boolean, s, ek9Float);
    assertEquals(expectedBase, mappedBase);
    //Now the next set to layer on top.
    List<ISymbol> typeArgumentsNext = List.of(ek9Float, ek9Time);
    var mappedNext = conceptualFlatteningMapping.apply(typeArgumentsNext, mappedBase);
    var expectedNext = List.of(ek9Integer, ek9Float, ek9Boolean, ek9Time, ek9Float);
    assertEquals(expectedNext, mappedNext);
    System.out.println(mappedNext);
  }
}

package org.ek9lang.compiler.symbols.generics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.ek9lang.compiler.support.PositionalConceptualLookupMapping;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.base.AbstractSymbolTestBase;
import org.junit.jupiter.api.Test;

/**
 * The multiple mappings of real and conceptual types when working on generics means
 * this test is essential to break the problem down and try lots of possible combinations.
 * This test on checks 'PositionalConceptualLookupMapping' and that is only used in 'TypeSubstitution'.
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

  private final PositionalConceptualLookupMapping conceptualLookupMapping = new PositionalConceptualLookupMapping();

  @Test
  void testSimpleConceptualMapping() {
    var genericType = new AggregateSymbol("GenericType", symbolTable);

    //(R, S) : (Float, String)
    var r = aggregateManipulator.createGenericT("R", genericType.getFullyQualifiedName(), symbolTable);
    var s = aggregateManipulator.createGenericT("S", genericType.getFullyQualifiedName(), symbolTable);
    genericType.addTypeParameterOrArgument(r);
    genericType.addTypeParameterOrArgument(s);

    //These are the parameters on the generic type
    List<ISymbol> typeParameters = List.of(r, s);
    //These are the type arguments passed in when parameterizing.
    List<ISymbol> typeArguments = List.of(ek9Float, ek9String);

    var mapping = conceptualLookupMapping.apply(typeArguments, typeParameters);
    //So now you can see - that it is possible to loop through obtains the generic conceptual type and find its
    //counterparty as the real parameterized type. I don't use a map for this because I want positional not value
    //based mapping.
    assertEquals(r, mapping.typeParameters().get(0));
    assertEquals(s, mapping.typeParameters().get(1));
    assertEquals(ek9Float, mapping.typeArguments().get(0));
    assertEquals(ek9String, mapping.typeArguments().get(1));
  }

  @Test
  void testMixedConcreteAndConceptualMapping() {
    //(Boolean, R, Duration, S, Integer) : (Float, String)
    var genericType = new AggregateSymbol("GenericType", symbolTable);

    var r = aggregateManipulator.createGenericT("R", genericType.getFullyQualifiedName(), symbolTable);
    var s = aggregateManipulator.createGenericT("S", genericType.getFullyQualifiedName(), symbolTable);
    genericType.addTypeParameterOrArgument(r);
    genericType.addTypeParameterOrArgument(s);

    //So this models a sort of partially parameterized type, with the additional concrete types now being provided.
    //These are the parameters on the generic type
    List<ISymbol> typeParameters = List.of(ek9Boolean, r, ek9Duration, s, ek9Integer);
    //These are the type arguments passed in when parameterizing.
    List<ISymbol> typeArguments = List.of(ek9Float, ek9String);

    var mapping = conceptualLookupMapping.apply(typeArguments, typeParameters);
    assertEquals(r, mapping.typeParameters().get(0));
    assertEquals(s, mapping.typeParameters().get(1));
    assertEquals(ek9Float, mapping.typeArguments().get(0));
    assertEquals(ek9String, mapping.typeArguments().get(1));
  }

  /**
   * The above are simple and obvious, but this one has a mix of real and conceptual types as parameters.
   * So the question is what should 'r' maps to and what should 's' map to?
   * The 'conceptualLookupMapping' pulls out the conceptual parameters and the new arguments to a map
   * to make this more obvious - so the positions align.
   */
  @Test
  void testConceptualToConceptualMapping() {
    //(Boolean, R, Duration, S, Integer) : (Float, T)
    var genericType = new AggregateSymbol("GenericType", symbolTable);

    var r = aggregateManipulator.createGenericT("R", genericType.getFullyQualifiedName(), symbolTable);
    var s = aggregateManipulator.createGenericT("S", genericType.getFullyQualifiedName(), symbolTable);
    var t = aggregateManipulator.createGenericT("T", genericType.getFullyQualifiedName(), symbolTable);
    genericType.addTypeParameterOrArgument(r);
    genericType.addTypeParameterOrArgument(s);
    genericType.addTypeParameterOrArgument(t);

    //While not really any different - here the mapping would result in a parameterized type
    //that would need to be parameterized further before being useful.

    //These are the parameters on the generic type
    List<ISymbol> typeParameters = List.of(ek9Boolean, r, ek9Duration, s, ek9Integer);
    //These are the type arguments passed in when parameterizing.
    List<ISymbol> typeArguments = List.of(ek9Float, t);

    var mapping = conceptualLookupMapping.apply(typeArguments, typeParameters);
    assertEquals(r, mapping.typeParameters().get(0));
    assertEquals(s, mapping.typeParameters().get(1));
    assertEquals(ek9Float, mapping.typeArguments().get(0));
    assertEquals(t, mapping.typeArguments().get(1));
  }

  @Test
  void testTooFewConceptualParameters() {
    //Note this time there is only one conceptual parameter, but I have erroneously passing two parameterizing args
    //These are the parameters on the generic type
    var genericType = new AggregateSymbol("GenericType", symbolTable);

    var r = aggregateManipulator.createGenericT("R", genericType.getFullyQualifiedName(), symbolTable);
    genericType.addTypeParameterOrArgument(r);

    List<ISymbol> typeParameters = List.of(ek9Boolean, r, ek9Duration, ek9Integer);
    //These are the type arguments passed in when parameterizing.
    List<ISymbol> typeArguments = List.of(ek9Float, ek9String);

    assertThrows(IllegalArgumentException.class, () -> conceptualLookupMapping.apply(typeArguments, typeParameters));
  }

  @Test
  void testTooFewParameterizingArguments() {
    //Note this time there are two conceptual parameter, but I have erroneously passed one parameterizing args
    //These are the parameters on the generic type
    var genericType = new AggregateSymbol("GenericType", symbolTable);
    var r = aggregateManipulator.createGenericT("R", genericType.getFullyQualifiedName(), symbolTable);
    var s = aggregateManipulator.createGenericT("S", genericType.getFullyQualifiedName(), symbolTable);
    genericType.addTypeParameterOrArgument(r);
    genericType.addTypeParameterOrArgument(s);

    List<ISymbol> typeParameters = List.of(ek9Boolean, r, s, ek9Integer);
    //These are the type arguments passed in when parameterizing.
    List<ISymbol> typeArguments = List.of(ek9Float);

    assertThrows(IllegalArgumentException.class, () -> conceptualLookupMapping.apply(typeArguments, typeParameters));
  }
}

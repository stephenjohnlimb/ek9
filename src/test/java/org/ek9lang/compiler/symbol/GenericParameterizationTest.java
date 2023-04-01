package org.ek9lang.compiler.symbol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.ek9lang.compiler.symbol.support.ParameterizedSymbolCreator;
import org.ek9lang.compiler.symbol.support.search.MethodSymbolSearch;
import org.ek9lang.compiler.symbol.support.search.SymbolSearch;
import org.ek9lang.compiler.symbol.support.search.TypeSymbolSearch;
import org.junit.jupiter.api.Test;

class GenericParameterizationTest extends AbstractSymbolTestBase {

  /**
   * While the ek9 compiler does not really use the
   * PossibleGenericSymbol class directly (normally via Aggregate or Function),
   * it may be useful to be able to use it outside of those contexts.
   * So here is a test for it.
   * It is quite flexible (complex) in its own right and is a bit of a
   * generic chameleon.
   */
  @Test
  void testPossibleGenericSymbolAsType() {
    var integerType = symbolTable.resolve(new TypeSymbolSearch("Integer"));
    assertTrue(integerType.isPresent());
    var stringType = symbolTable.resolve(new TypeSymbolSearch("String"));
    assertTrue(stringType.isPresent());

    var aGenericType = testCreationOfGenericType("G1", "T");
    assertNotNull(aGenericType);

    //Just check cloning and equality work OK
    var clonedGenericType1 = aGenericType.clone(symbolTable);
    assertEquals(aGenericType, clonedGenericType1);
    assertEquals(aGenericType.hashCode(), clonedGenericType1.hashCode());
    //and self
    assertEquals(aGenericType, aGenericType);

    //Just symbol 'public T <- aMethod(arg1 as T)'
    assertEquals(1, aGenericType.getSymbolsForThisScope().size());

    //Let's simulate the fact that internally aGenericType uses another
    //sort of generic type - it could be in a code block, or an input parameter or a return parameter.
    //We're not actually going to do anything with it here - yet - just like the replacement of 'T' -> 'Integer'
    //is done elsewhere.
    var dependentGenericType = testCreationOfGenericType("Dep1", "Q");
    aGenericType.addGenericSymbolReference(dependentGenericType);
    //So what does the above mean - it means when we parameterize 'AGenericType' with 'Integer' thereby replacing
    //'T' -> 'Integer' - we will also need to do the same for 'Dep1' but replace 'Q' -> 'Integer'.
    //We ONLY addGenericSymbolReference - for generic types that are still conceptual.
    //If they are concrete we DON'T use addGenericSymbolReference. i.e. if our block of code in
    //AGenericType has used 'Dep1' but had actually given it a concrete parameter like 'Date' or 'Integer'
    //That is NOT a 'dependent' generic type, so we would not add it as a reference.

    //Now as we've altered aGenericType lets equality again
    assertNotEquals(aGenericType, clonedGenericType1);
    assertNotEquals(aGenericType.hashCode(), clonedGenericType1.hashCode());
    var clonedGenericType2 = aGenericType.clone(symbolTable);
    assertEquals(aGenericType, clonedGenericType2);
    assertEquals(aGenericType.hashCode(), clonedGenericType2.hashCode());
  }

  @Test
  void testPossibleGenericSymbolAsFunction() {
    var integerType = symbolTable.resolve(new TypeSymbolSearch("Integer"));
    assertTrue(integerType.isPresent());
    var stringType = symbolTable.resolve(new TypeSymbolSearch("String"));
    assertTrue(stringType.isPresent());

    var aGenericFunction = testCreationOfGenericFunction("F2", "S");
    assertNotNull(aGenericFunction);

    //No need for further tests as we are just checking the slight differences between TYPE and FUNCTION.
  }

  /**
   * Just checking that Function can be parameterised.
   */
  @Test
  void testConcreteParameterizationAsFunction() {
    var integerType = symbolTable.resolve(new TypeSymbolSearch("Integer"));
    assertTrue(integerType.isPresent());
    var stringType = symbolTable.resolve(new TypeSymbolSearch("String"));
    assertTrue(stringType.isPresent());

    var aGenericType = testCreationOfGenericType("F1", "T");

    var parameterizedWithInteger = testGenericParameterization(aGenericType, "T", integerType.get());

    //let's also check that cloning of parameterized type works.
    var clonedParameterizedWithInteger = parameterizedWithInteger.clone(symbolTable);
    assertEquals(parameterizedWithInteger, clonedParameterizedWithInteger);
    assertEquals(parameterizedWithInteger.hashCode(), clonedParameterizedWithInteger.hashCode());

    var parameterizedWithString = testGenericParameterization(aGenericType, "T", ek9String);
    assertNotNull(parameterizedWithString);
    assertNotEquals(parameterizedWithInteger, parameterizedWithString);
  }

  /**
   * Tests a generic type that accepts a single type parameter can be used with a real concrete type argument.
   */
  @Test
  void testConcreteParameterizationAsType() {
    var integerType = symbolTable.resolve(new TypeSymbolSearch("Integer"));
    assertTrue(integerType.isPresent());
    var stringType = symbolTable.resolve(new TypeSymbolSearch("String"));
    assertTrue(stringType.isPresent());

    //Plan to use this as a delegate and see if I can use it to parameterize a generic type.
    FunctionSymbol fn = new FunctionSymbol("someFunction", symbolTable);
    assertNotNull(fn);
    assertTrue(fn.getType().isPresent());

    var aGenericType = testCreationOfGenericType("G1", "T");

    var parameterizedWithInteger = testGenericParameterization(aGenericType, "T", integerType.get());

    //let's also check that cloning of parameterized type works.
    var clonedParameterizedWithInteger = parameterizedWithInteger.clone(symbolTable);
    assertEquals(parameterizedWithInteger, clonedParameterizedWithInteger);
    assertEquals(parameterizedWithInteger.hashCode(), clonedParameterizedWithInteger.hashCode());

    var parameterizedWithString = testGenericParameterization(aGenericType, "T", ek9String);
    assertNotNull(parameterizedWithString);
    assertNotEquals(parameterizedWithInteger, parameterizedWithString);

    var parameterizedWithFunction = testGenericParameterization(aGenericType, "T", fn.getType().get());
    assertNotNull(parameterizedWithFunction);
    assertNotEquals(parameterizedWithInteger, parameterizedWithFunction);
  }

  @Test
  void testConceptualParameterizationAsFunction() {
    var p = support.createGenericT("P", symbolTable);
    assertTrue(p.isConceptualTypeParameter());

    var aGenericFunction = testCreationOfGenericFunction("F1", "T");

    var parameterizedWithConceptualType = testGenericParameterization(aGenericFunction, "T", p);

    //let's also check that cloning of parameterized type works.
    var clonedParameterizedWithConceptualType = parameterizedWithConceptualType.clone(symbolTable);
    assertEquals(parameterizedWithConceptualType, clonedParameterizedWithConceptualType);
    assertEquals(parameterizedWithConceptualType.hashCode(), clonedParameterizedWithConceptualType.hashCode());
  }

  /**
   * Now check that the generic type with a single type parameter can be parameterized with a 'conceptual' type argument.
   */
  @Test
  void testConceptualParameterizationAsType() {
    var p = support.createGenericT("P", symbolTable);
    assertTrue(p.isConceptualTypeParameter());

    var aGenericType = testCreationOfGenericType("G1", "T");

    var parameterizedWithConceptualType = testGenericParameterization(aGenericType, "T", p);

    //let's also check that cloning of parameterized type works.
    var clonedParameterizedWithConceptualType = parameterizedWithConceptualType.clone(symbolTable);
    assertEquals(parameterizedWithConceptualType, clonedParameterizedWithConceptualType);
    assertEquals(parameterizedWithConceptualType.hashCode(), clonedParameterizedWithConceptualType.hashCode());
  }

  @Test
  void testWithMultipleTypeParameters() {
    var tripleGenericType = testCreateGenericTypeWithMultipleParameters("TripleType", List.of("Q", "R", "S"));
    assertNotNull(tripleGenericType);
    assertEquals(3, tripleGenericType.getAnyConceptualTypeParameters().size());

    var quadGenericType = testCreateGenericTypeWithMultipleParameters("QuadType", List.of("P", "Q", "R", "S"));
    assertNotNull(quadGenericType);
    assertEquals(4, quadGenericType.getAnyConceptualTypeParameters().size());
  }

  @Test
  void testMultipleConceptualTypeParameters() {
    var integerType = symbolTable.resolve(new TypeSymbolSearch("Integer"));
    assertTrue(integerType.isPresent());
    var stringType = symbolTable.resolve(new TypeSymbolSearch("String"));
    assertTrue(stringType.isPresent());
    var durationType = symbolTable.resolve(new TypeSymbolSearch("Duration"));
    assertTrue(durationType.isPresent());

    ParameterizedSymbolCreator creator = new ParameterizedSymbolCreator();

    var aGenericType = testCreateGenericTypeWithMultipleParameters("AType", List.of("R", "S"));
    assertEquals(2, aGenericType.getAnyConceptualTypeParameters().size());
    assertNotNull(aGenericType);

    //Now parameterize it with concrete types
    var parameterizedType = creator.apply(aGenericType, List.of(integerType.get(), ek9String));
    assertNotNull(parameterizedType);
    assertEquals(ISymbol.SymbolCategory.TYPE, parameterizedType.getCategory());

    //Now lets try a sort of half way house of parameterizing one concrete and on conceptual.
    //This simulates doing something within a generic type itself, where:
    /*
    AType of type (R, S)
      ...

    SomeType of type (K, V)
      ...
      someMethod()
        myType as AType of (Integer, V): AType()
     */
    //So here we are modelling that use of 'myType' - because while it have been parameterized
    //It still needs a 'concrete' type to be useful and that can only be done when 'SomeType of type (K, V)'
    //Is actually employed like: 'someType as SomeType of (Date, Duration)' for example.
    //As that point 'AType of (Integer, V)' would again be parameterized - but only with 'V' (Duration).
    //So we'd end up with yet another parameterized type!! AType of (Integer, Duration).

    var v = support.createGenericT("V", symbolTable);
    //This should still be conceptual!
    var conceptualParameterizedType = creator.apply(aGenericType, List.of(integerType.get(), v));
    assertNotNull(conceptualParameterizedType);
    assertEquals(1, conceptualParameterizedType.getAnyConceptualTypeParameters().size());
    assertEquals( ISymbol.SymbolCategory.TEMPLATE_TYPE, conceptualParameterizedType.getCategory());

    //So now I'd need to find which conceptual parameter to change - should be the second one
    //'someType as SomeType of (Date, Duration)' -> 'AType of (Integer, Duration)'
    int indexOfV = indexFinder.apply(conceptualParameterizedType, v);
    assertEquals(1, indexOfV);

    //Finally I can create the AType of (Integer, Duration)
    //This is what the outer type would be passing in/
    var p = durationType.get();
    //But now there will only be a need for a single parameter to be defined because 'Integer' is already wired in
    var integerDurationParameterizedType = creator.apply(conceptualParameterizedType, List.of(p));
    assertNotNull(integerDurationParameterizedType);
    //It will now be a property type rather than a template.
    assertEquals(ISymbol.SymbolCategory.TYPE, integerDurationParameterizedType.getCategory());

    //Summary, the generic type => to a parameterized but still generic type => concrete type
    //AType of type (R, S) => AType of (Integer, V) => AType of (Integer, Duration)
    //Now all of the above needs to be wrapped up in to 'function' and also the method population.
    //HARD - I find this quite hard.
  }

  /**
   * Accepts a generic Type and parameterizes it with a type parameter.
   */
  private PossibleGenericSymbol testGenericParameterization(final PossibleGenericSymbol aGenericType,
                                                            @SuppressWarnings("SameParameterValue")
                                                            final String conceptualTypeParameterName,
                                                            final ISymbol typeParameter) {

    //Now use and tests the creator of parameterized types/functions.
    ParameterizedSymbolCreator creator = new ParameterizedSymbolCreator();

    var aParameterizedType = creator.apply(aGenericType, List.of(typeParameter));

    //Now if this method was passed in a conceptual type, even the new parameterized type will be generic.
    //I know this sounds strange, but imagine inside a generic type, that uses generic types with T and K and V
    //Even though the code is parameterizing a generic type, only when the outermost generic type is parameterized
    //with concrete types will all those dependent generic (and parameterized but still generic) types become truly
    //parameterized with concrete types.

    //Could have been written more concisely - but I want to make it obvious.
    if (typeParameter.isConceptualTypeParameter()) {
      assertTrue(aParameterizedType.isGenericInNature());
    } else {
      assertFalse(aParameterizedType.isGenericInNature());
    }

    //Now lets check that from a parameterised type point of view - 'T' is not visible/resolvable
    var notResolvedT = aParameterizedType.resolve(new TypeSymbolSearch(conceptualTypeParameterName));
    assertTrue(notResolvedT.isEmpty());

    //Now logically at this point you'd be expecting that as AGenericType of type T has a method:
    //public T <- aMethod(arg1 as T)
    //A parameterized version of AGenericType with T -> Integer would obviously result in a method:
    //public Integer <- aMethod(arg1 as Integer)
    //Well you'd be disappointed! - lets check there are zero
    assertEquals(0, aParameterizedType.getSymbolsForThisScope().size());

    //Why? It's not logical - in an 'OO' manner the act of parameterization should 'hydrate' the template
    //'aGenericType' and 'search and replace all the 'T' with 'Integer'.
    //Well you might think so - I did that first in the prototypes and its bad news I'm afraid.

    //If you go down the route above, you end up with loads of circular dependencies and multiple new types needing to
    //be created - but before a new main type has been recorded. This means a stack overflow.

    //The best way I could find of doing this, was to:
    //1. Do as above - create the parameterized type - but leave the 'search and replace of 'T' -> 'Integer'
    //2. Register that new parameterized type in the appropriate 'module/scope'
    //Do this for all dependent types: see aGenericType.getGenericSymbolReferences()
    //This means all types are registered - but are not fully populated with their parameterized methods
    //3. Now go through all those parameterized types and do the 'T' -> 'Integer' mapping on all the methods.

    return aParameterizedType;
  }

  /**
   * Now make a possible generic symbol but mark it as a FUNCTION.
   */
  private PossibleGenericSymbol testCreationOfGenericFunction(final String genericFunctionName,
                                                              final String conceptualTypeParameterName) {
    var t = support.createGenericT(conceptualTypeParameterName, symbolTable);
    assertTrue(t.isConceptualTypeParameter());

    //This will be our generic function that has one or more 'type parameters'
    var aGenericFunction = new PossibleGenericSymbol(genericFunctionName, symbolTable);
    //just for the test use the symbol table.
    aGenericFunction.setModuleScope(symbolTable);

    //Normally the symbol factory would do this bit for us.
    //When we add a parameter it will become a template type
    aGenericFunction.setCategory(ISymbol.SymbolCategory.FUNCTION);
    assertFalse(aGenericFunction.isGenericInNature());
    aGenericFunction.addTypeParameterOrArgument(t);
    assertTrue(aGenericFunction.isGenericInNature());
    //And its category will have changed to the template version.
    assertEquals(ISymbol.SymbolCategory.TEMPLATE_FUNCTION, aGenericFunction.getCategory());

    var resolvedT = aGenericFunction.resolve(new TypeSymbolSearch(conceptualTypeParameterName));
    assertTrue(resolvedT.isPresent());
    assertEquals(t, resolvedT.get());

    //Check the argument we are about to add to the 'function' cannot be found.
    var search = new SymbolSearch("arg1");
    var notResolvedArg = aGenericFunction.resolve(search);
    assertTrue(notResolvedArg.isEmpty());

    var arg1 = new VariableSymbol("arg1", t);
    //Now let's define an incoming parameter
    aGenericFunction.define(arg1);

    var resolvedArg = aGenericFunction.resolve(search);
    assertTrue(resolvedArg.isPresent());
    assertEquals(arg1, resolvedArg.get());

    return aGenericFunction;
  }

  /**
   * Designed to just create a Generic type with one type parameter and a single
   * method that uses that type parameter.
   * The creation steps are checked.
   */
  private PossibleGenericSymbol testCreationOfGenericType(final String genericTypeName,
                                                          final String conceptualTypeParameterName) {
    //Make a 'conceptual' type like 'T' or 'K' or 'V'.
    var t = support.createGenericT(conceptualTypeParameterName, symbolTable);
    assertTrue(t.isConceptualTypeParameter());

    //This will be our generic type that has one or more 'type parameters'
    var aGenericType = new PossibleGenericSymbol(genericTypeName, symbolTable);
    //just for the test use the symbol table.
    aGenericType.setModuleScope(symbolTable);

    //Normally the symbol factory would do this bit for us.
    //When we add a parameter it will become a template type
    aGenericType.setCategory(ISymbol.SymbolCategory.TYPE);
    assertFalse(aGenericType.isGenericInNature());

    //So this is the important bit that makes it a generic type
    //By adding a conceptual type parameter it 'becomes' generic
    aGenericType.addTypeParameterOrArgument(t);
    assertTrue(aGenericType.isGenericInNature());
    //And its category will have changed to the template version.
    assertEquals(ISymbol.SymbolCategory.TEMPLATE_TYPE, aGenericType.getCategory());

    //Now we will add a method to the generic type it will accept a 'T' and return a 'T'
    var method = new MethodSymbol("aMethod", aGenericType);
    method.define(new VariableSymbol("arg1", t));
    method.setReturningSymbol(new VariableSymbol("rtn", t));
    aGenericType.define(method);

    //See if we can resolve 'T' from within the generic type
    var resolvedT = aGenericType.resolve(new TypeSymbolSearch(conceptualTypeParameterName));
    assertTrue(resolvedT.isPresent());
    assertEquals(t, resolvedT.get());

    //Check we cannot resolve the method - without the correct parameters.
    var methodSearch = new MethodSymbolSearch("aMethod");
    var notResolvedMethod = aGenericType.resolve(methodSearch);
    assertTrue(notResolvedMethod.isEmpty());

    //Now add the parameter type and check we can resolve it.
    methodSearch.addTypeParameter(t);
    var resolvedMethod = aGenericType.resolve(methodSearch);
    assertTrue(resolvedMethod.isPresent());
    assertEquals(method, resolvedMethod.get());

    return aGenericType;
  }

  private PossibleGenericSymbol testCreateGenericTypeWithMultipleParameters(final String genericTypeName,
                                                   final List<String> conceptualTypeParameterNames) {
    var aGenericType = new PossibleGenericSymbol(genericTypeName, symbolTable);
    aGenericType.setModuleScope(symbolTable);
    aGenericType.setCategory(ISymbol.SymbolCategory.TYPE);

    //OK now the multiple type parameters
    conceptualTypeParameterNames.forEach(typeParameterName -> {
      var typeParameter = support.createGenericT(typeParameterName, symbolTable);
      aGenericType.addTypeParameterOrArgument(typeParameter);
    });
    //This should now have become a template version
    assertEquals(ISymbol.SymbolCategory.TEMPLATE_TYPE, aGenericType.getCategory());

    //It might seem strange to do this, but all typeParameters should be conceptual.
    assertEquals(aGenericType.getAnyConceptualTypeParameters(), aGenericType.getTypeParameterOrArguments());

    return aGenericType;
  }
}

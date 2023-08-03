package org.ek9lang.compiler.symbols;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.ek9lang.compiler.ParametricResolveOrDefine;
import org.ek9lang.compiler.search.AnyTypeSymbolSearch;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.search.SymbolSearch;
import org.ek9lang.compiler.search.TypeSymbolSearch;
import org.ek9lang.compiler.support.ParameterizedSymbolCreator;
import org.ek9lang.compiler.support.TypeSubstitution;
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
  @SuppressWarnings("EqualsWithItself")
  @Test
  void testPossibleGenericSymbolAsType() {
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
  void testAbstractGenericType() {

    var parametricResolveOrDefine = new ParametricResolveOrDefine(symbolTable);
    var typeSubstitution = new TypeSubstitution(parametricResolveOrDefine);

    var aGenericType = creationAbstractGenericType();
    assertTrue(aGenericType.isMarkedAbstract());
    assertEquals(1, aGenericType.getSymbolsForThisScope().size());
    var theMethod = (MethodSymbol) aGenericType.getSymbolsForThisScope().get(0);
    assertTrue(theMethod.isMarkedAbstract());

    //Now let's use an integer to parameterize this abstract generic class, to give use something that is
    //type safe as an integer, but it should still be abstract both as class and method level.
    var parameterizedGenericTypeWithInteger = testGenericParameterization(aGenericType, "T", ek9Integer);
    assertNotNull(parameterizedGenericTypeWithInteger);
    assertTrue(parameterizedGenericTypeWithInteger.isMarkedAbstract());
    assertFalse(parameterizedGenericTypeWithInteger.isGenericInNature());

    //Now trigger the type substitution.
    var mutatedParameterizedGenericTypeWithInteger = typeSubstitution.apply(parameterizedGenericTypeWithInteger);

    //Now lets get the method - even though it was abstract - it should now be of T -> Integer and still be abstract.
    assertEquals(1, mutatedParameterizedGenericTypeWithInteger.getSymbolsForThisScope().size());
    var parameterisedMethod = (MethodSymbol) mutatedParameterizedGenericTypeWithInteger.getSymbolsForThisScope().get(0);
    assertTrue(parameterisedMethod.isMarkedAbstract());

    assertTrue(parameterisedMethod.getType().isPresent());
    assertEquals(ek9Integer, parameterisedMethod.getType().get());

    assertTrue(parameterisedMethod.getReturningSymbol().isReturningParameter());
    assertTrue(parameterisedMethod.getReturningSymbol().getType().isPresent());
    assertEquals(ek9Integer, parameterisedMethod.getReturningSymbol().getType().get());

  }

  @Test
  void testPossibleGenericSymbolAsFunction() {
    var aGenericFunction = testCreationOfGenericFunction("F2", "S");
    assertNotNull(aGenericFunction);

    //No need for further tests as we are just checking the slight differences between TYPE and FUNCTION.
  }

  /**
   * Just checking that Function can be parameterised.
   */
  @Test
  void testConcreteParameterizationAsFunction() {
    var aGenericType = testCreationOfGenericType("F1", "T");
    var parameterizedWithInteger = testGenericParameterization(aGenericType, "T", ek9Integer);

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
    //Plan to use this as a delegate and see if I can use it to parameterize a generic type.
    FunctionSymbol fn = new FunctionSymbol("someFunction", symbolTable);
    assertNotNull(fn);
    assertTrue(fn.getType().isPresent());

    var aGenericType = testCreationOfGenericType("G1", "T");

    var parameterizedWithInteger = testGenericParameterization(aGenericType, "T", ek9Integer);

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

    ParameterizedSymbolCreator creator = new ParameterizedSymbolCreator();

    var aGenericType = testCreateGenericTypeWithMultipleParameters("AType", List.of("R", "S"));
    assertEquals(2, aGenericType.getAnyConceptualTypeParameters().size());
    assertNotNull(aGenericType);
    assertTrue(aGenericType.isGenericInNature());
    assertTrue(aGenericType.isConceptualTypeParameter());

    //Now parameterize it with concrete types
    var parameterizedType = creator.apply(aGenericType, List.of(ek9Integer, ek9String));
    assertNotNull(parameterizedType);
    assertEquals(ISymbol.SymbolCategory.TYPE, parameterizedType.getCategory());
    assertFalse(parameterizedType.isGenericInNature());
    assertFalse(parameterizedType.isConceptualTypeParameter());

    //Now lets try a sort of half way house of parameterizing one concrete and one conceptual.
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
    var conceptualParameterizedType = creator.apply(aGenericType, List.of(ek9Integer, v));
    assertNotNull(conceptualParameterizedType);
    assertEquals(1, conceptualParameterizedType.getAnyConceptualTypeParameters().size());
    assertEquals(ISymbol.SymbolCategory.TEMPLATE_TYPE, conceptualParameterizedType.getCategory());

    //Finally I can create the AType of (Integer, Duration)
    //This is what the outer type would be passing in/
    var p = ek9Duration;
    //But now there will only be a need for a single parameter to be defined because 'Integer' is already wired in
    var integerDurationParameterizedType = creator.apply(conceptualParameterizedType, List.of(p));
    assertNotNull(integerDurationParameterizedType);
    //It will now be a property type rather than a template.
    assertEquals(ISymbol.SymbolCategory.TYPE, integerDurationParameterizedType.getCategory());

    //Summary, the generic type => to a parameterized but still generic type => concrete type
    //AType of type (R, S) => AType of (Integer, V) => AType of (Integer, Duration)
    //HARD - I find this quite hard.
  }

  /**
   * Accepts a generic Type/Function and parameterizes it with a type argument.
   */
  private PossibleGenericSymbol testGenericParameterization(final PossibleGenericSymbol aGenericType,
                                                            @SuppressWarnings("SameParameterValue")
                                                            final String conceptualTypeParameterName,
                                                            final ISymbol typeArgument) {

    //Now use and tests the creator of parameterized types/functions.
    ParameterizedSymbolCreator creator = new ParameterizedSymbolCreator();

    var aParameterizedType = creator.apply(aGenericType, List.of(typeArgument));

    //Now if this method was passed in a conceptual type, even the new parameterized type will be generic.
    //I know this sounds strange, but imagine inside a generic type, that uses generic types with T and K and V
    //Even though the code is parameterizing a generic type, only when the outermost generic type is parameterized
    //with concrete types will all those dependent generic (and parameterized but still generic) types become truly
    //parameterized with concrete types.

    //Could have been coded more concisely - but I want to make it obvious.
    if (typeArgument.isConceptualTypeParameter()) {
      assertTrue(aParameterizedType.isGenericInNature());
    } else {
      assertFalse(aParameterizedType.isGenericInNature());
    }

    //Now lets check that from a parameterised type point of view - 'T' is not visible/resolvable
    //This should be obvious - we don;t want 'conceptual types' bleeding out. So check they don't.
    var notResolvedT = aParameterizedType.resolve(new AnyTypeSymbolSearch(conceptualTypeParameterName));
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
    //be created - but need to be created before a new main type has been recorded. This means a stack overflow.

    //The best way I could find of doing this, was to:
    //1. Do as above - create the parameterized type - but leave the 'search and replace of 'T' -> 'Integer' for later
    //2. Register that new parameterized type in the appropriate 'module/scope' - even though it incomplete.
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

    //Check the argument we are about to add to the 'function' is not currently present.
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

  private PossibleGenericSymbol creationAbstractGenericType() {
    var t = support.createGenericT("AbsG1", symbolTable);
    var aGenericType = new PossibleGenericSymbol("T", symbolTable);
    aGenericType.setModuleScope(symbolTable);
    aGenericType.setCategory(ISymbol.SymbolCategory.TYPE);
    aGenericType.addTypeParameterOrArgument(t);

    //Now we want to make this 'abstract' because the method we plan to provide will also be abstract
    //So what this means is that this generic type can be parameterised with say T -> String
    //But it still cannot be instantiated as it should be abstract, hence can only be extended from.
    aGenericType.setMarkedAbstract(true);

    //So now let's add in that abstract method.
    //Now we will add a method to the generic type it will accept a 'T' and return a 'T'
    var method = new MethodSymbol("aMethod", aGenericType);
    method.setMarkedAbstract(true);
    method.define(new VariableSymbol("arg1", t));
    method.setReturningSymbol(new VariableSymbol("rtn", t));
    aGenericType.define(method);

    //OK all done
    return aGenericType;
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
    //When making a new possible generic type - it starts out as non-generic.
    aGenericType.setModuleScope(symbolTable);
    aGenericType.setCategory(ISymbol.SymbolCategory.TYPE);

    //OK now the multiple type parameters - this act makes it generic as it now has 'type parameters'.
    conceptualTypeParameterNames.forEach(typeParameterName -> {
      var typeParameter = support.createGenericT(typeParameterName, symbolTable);
      aGenericType.addTypeParameterOrArgument(typeParameter);
    });

    //This should now have become a template version - i.e. it is now generic and can be parameterised with
    //'type arguments'.
    assertEquals(ISymbol.SymbolCategory.TEMPLATE_TYPE, aGenericType.getCategory());

    //It might seem strange to do check this, but all typeParameters should be conceptual.
    assertEquals(aGenericType.getAnyConceptualTypeParameters(), aGenericType.getTypeParameterOrArguments());

    return aGenericType;
  }
}

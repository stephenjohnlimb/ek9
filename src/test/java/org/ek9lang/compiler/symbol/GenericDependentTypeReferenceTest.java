package org.ek9lang.compiler.symbol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.ek9lang.compiler.internals.ParametricResolveOrDefine;
import org.ek9lang.compiler.symbol.support.ParameterizedSymbolCreator;
import org.ek9lang.compiler.symbol.support.TypeSubstitution;
import org.ek9lang.compiler.symbol.support.search.TypeSymbolSearch;
import org.junit.jupiter.api.Test;

/**
 * Designed to test types that are dependent when defining a generic type.
 * Sample of what the EK9 source would be to try this out.
 * But in this test it is handcrafted, so we can check the building blocks work.
 * <pre>
 * SomeGeneric of type (K, V)
 *  //Just add one method on this.
 *  lookup()
 *    -> arg0 as K
 *    <- rtn as V
 * ...
 * AnotherGeneric of type (R, S)
 *  someMethod()
 *    //So this below is a parameterised type with whatever R and S are going to be
 *    //But it is still 'conceptual' - hence a reference to SomeGeneric is needed
 *    -> arg0 as SomeGeneric of (R, S)
 *  anotherMethod()
 *    //This is still conceptual because it uses S - hence a reference to SomeGeneric is needed
 *    -> arg0 as SomeGeneric of (Integer, S)
 *  concreteMethod()
 *    //But this results in something that can be parameterised and does NOT depend on R or S
 *    -> arg0 as SomeGeneric of (Integer, Duration)
 *
 *  ...
 *  YetAnotherGeneric of type (X, Y)
 *    yetAnotherMethod()
 *      // Reference to AnotherGeneric will be needed as it depends on 'Y'
 *      g1 as AnotherGeneric of (Date, Y)
 *      // Reference to AnotherGeneric will be needed as it depends on 'X'
 *      g2 as AnotherGeneric of (X, Boolean)
 *      //Or more complex
 *      g3 as AnotherGeneric of (String, AnotherGeneric of (X, Y))
 *
 * ...
 *  //Now use that AnotherGeneric, with a Float and a String
 *  anotherGenericOfFloatandString as AnotherGeneric of (Float, String): ...
 *  //And also use YetAnotherGeneric, with Dimension and Time
 *  yetAnotherGenericOfDimensionAndTime as YetAnotherGeneric of (Dimension, Time): ...
 *
 * </pre>
 * I'm still unsure about using references too early in the compiler phases.
 * The input and output types are all OK, because they are 'visible' and so can be created.
 * So we need to record SomeGeneric of (R, S) and SomeGeneric of (Integer, S) in the references of AnotherGeneric
 * That way when we say AnotherGeneric of (Float, String) we can also create:
 * SomeGeneric of (Float, String) and SomeGeneric of (Integer, String)
 * But note AnotherGeneric - has also been used in 'YetAnotherGeneric' with one or more of its type parameters
 * We'd need to keep at track of any conceptual generic types that resulted as well. So
 * when YetAnotherGeneric is parameterised with (Time, DateTime) that too can ripple through.
 * So this test is the long hand mechanism that the compiler and support components will use.
 * Here I'm just still getting my head around it and ensuring that the building blocks will actually work.
 * There is 'chaining' going on here, dependent references and also partial types that need to be applied.
 */
class GenericDependentTypeReferenceTest extends AbstractSymbolTestBase {

  private final ParameterizedSymbolCreator creator = new ParameterizedSymbolCreator();

  private TypeSubstitution typeSubstitution;

  /**
   * This mirrors the ek9 sample source code above and checks it can all be wired together.
   */
  @Test
  void testDependentGenericTypes() {
    //This just simulates the CompilableProgram.
    ParametricResolveOrDefine parametricResolveOrDefine = new ParametricResolveOrDefine(symbolTable);

    typeSubstitution = new TypeSubstitution(parametricResolveOrDefine);

    //make the generics outlines in the ek9 source snip above.
    var SomeGenericOfKandV = createSomeGenericOfKandV();
    assertNotNull(SomeGenericOfKandV);

    var AnotherGenericOfRandS = createAnotherGenericOfRandS(SomeGenericOfKandV);
    assertNotNull(AnotherGenericOfRandS);
    //There should be three methods.
    assertEquals(3, AnotherGenericOfRandS.getSymbolsForThisScope().size());

    //Now need to make 'YetAnotherGeneric of type (X, Y)'
    var YetAnotherGenericOfXandY = createYetAnotherGenericOfXandY(AnotherGenericOfRandS);
    assertNotNull(YetAnotherGenericOfXandY);
    assertTrue(YetAnotherGenericOfXandY.isGenericInNature());
    assertTrue(YetAnotherGenericOfXandY.isConceptualTypeParameter());
    assertEquals(1, YetAnotherGenericOfXandY.getSymbolsForThisScope().size());

    //Should/Could have references to:
    //'AnotherGeneric of (Date, Y)'
    //'AnotherGeneric of (X, Boolean)'
    //'AnotherGeneric of (String, AnotherGeneric of (X, Y))'
    //assertEquals(3, YetAnotherGenericOfXandY.getGenericSymbolReferences().size());

    //Now lets make the concrete version using: AnotherGeneric of (Float, String)
    var anotherGenericOfFloatandString = createAnotherGenericOfFloatandString(AnotherGenericOfRandS);
    assertNotNull(anotherGenericOfFloatandString);
    assertFalse(anotherGenericOfFloatandString.isGenericInNature());
    //We'd also expect 3 methods with the right types in there
    assertEquals(3, anotherGenericOfFloatandString.getSymbolsForThisScope().size());

    //Finally need to make 'yetAnotherGenericOfDimensionAndTime as YetAnotherGeneric of (Dimension, Time)'
    var yetAnotherGenericOfDimensionandTime = createYetAnotherGenericOfDimensionandTime(YetAnotherGenericOfXandY);
    assertNotNull(yetAnotherGenericOfDimensionandTime);
    assertNotNull(yetAnotherGenericOfDimensionandTime);
    assertFalse(yetAnotherGenericOfDimensionandTime.isGenericInNature());
    //We'd also expect 1 method with the right types in there
    assertEquals(1, yetAnotherGenericOfDimensionandTime.getSymbolsForThisScope().size());

    //TODO create a new generic type resolver and add in the assertions.

    symbolTable.getSymbolsForThisScope().forEach(System.out::println);
  }

  /**
   * Helper method to provide the 'SomeGeneric type of (K, V)'.
   * See the conceptual example at the top of this file.
   */
  private PossibleGenericSymbol createSomeGenericOfKandV() {
    var k = support.createGenericT("K", symbolTable);
    var v = support.createGenericT("V", symbolTable);

    var someGenericOfKandV = new AggregateSymbol("SomeGeneric", symbolTable);
    someGenericOfKandV.setModuleScope(symbolTable);
    someGenericOfKandV.addTypeParameterOrArgument(k);
    someGenericOfKandV.addTypeParameterOrArgument(v);

    assertTrue(someGenericOfKandV.isGenericInNature());
    assertTrue(someGenericOfKandV.isConceptualTypeParameter());

    addLookupMethod(someGenericOfKandV);

    return someGenericOfKandV;
  }

  /**
   * Helper method to provide 'AnotherGeneric type of (R, S)'.
   * This needs the someGenericOfKandV type to be able to populate the methods it defines.
   */
  private PossibleGenericSymbol createAnotherGenericOfRandS(final PossibleGenericSymbol someGenericOfKandV) {
    //So starts out same as above - I've done this 'long hand' so it's quite obvious.
    var r = support.createGenericT("R", symbolTable);
    var s = support.createGenericT("S", symbolTable);

    var anotherGenericRandS = new AggregateSymbol("AnotherGeneric", symbolTable);
    anotherGenericRandS.setModuleScope(symbolTable);
    anotherGenericRandS.addTypeParameterOrArgument(r);
    anotherGenericRandS.addTypeParameterOrArgument(s);

    assertTrue(someGenericOfKandV.isGenericInNature());
    assertTrue(someGenericOfKandV.isConceptualTypeParameter());

    addSomeMethod(someGenericOfKandV, anotherGenericRandS);
    addAnotherMethod(someGenericOfKandV, anotherGenericRandS);
    addConcreteMethod(someGenericOfKandV, anotherGenericRandS);

    return anotherGenericRandS;
  }

  private PossibleGenericSymbol createYetAnotherGenericOfXandY(final PossibleGenericSymbol anotherGenericOfRandS) {
    //Again here we go - this is what the compiler will do for - but we need to ensure that everything gets wired up
    var x = support.createGenericT("X", symbolTable);
    var y = support.createGenericT("Y", symbolTable);

    var yetAnotherGenericOfXandY = new AggregateSymbol("YetAnotherGeneric", symbolTable);
    yetAnotherGenericOfXandY.setModuleScope(symbolTable);
    yetAnotherGenericOfXandY.addTypeParameterOrArgument(x);
    yetAnotherGenericOfXandY.addTypeParameterOrArgument(y);

    assertTrue(yetAnotherGenericOfXandY.isGenericInNature());
    assertTrue(yetAnotherGenericOfXandY.isConceptualTypeParameter());

    addYetAnotherMethod(yetAnotherGenericOfXandY, x, y, anotherGenericOfRandS);

    return yetAnotherGenericOfXandY;
  }

  /**
   * Add a method with no parameters - this is where we simulate adding references.
   * The point being the input parameters and return parameters are 'visible' on the symbol signatures.
   * But code that is used within the body is hidden (implementation detail). Ideally we'd like to create these
   * types as early as possible. This will enable the compiler to detect 'missing operators' when instantiating
   * generics. As we make the assumption in generic code that 'all operators' are available.
   */
  private void addYetAnotherMethod(PossibleGenericSymbol yetAnotherGenericOfXandY,
                                   final ISymbol x, final ISymbol y,
                                   final PossibleGenericSymbol anotherGenericOfRandS) {
    var yetAnotherMethod = new MethodSymbol("yetAnotherMethod", yetAnotherGenericOfXandY);
    yetAnotherMethod.setReturningSymbol(ek9Void);
    yetAnotherGenericOfXandY.define(yetAnotherMethod);
    //Now to simulate parsing the method body and registering the dependent types.

    //'g1 as AnotherGeneric of (Date, Y)'
    //var g1 = creator.apply(anotherGenericOfRandS, List.of(ek9Date, y));
    //yetAnotherGenericOfXandY.addGenericSymbolReference(typeSubstitution.apply(g1));

    //'g2 as AnotherGeneric of (X, Boolean)'
    //var g2 = creator.apply(anotherGenericOfRandS, List.of(x, ek9Boolean));
    //yetAnotherGenericOfXandY.addGenericSymbolReference(typeSubstitution.apply(g2));

    //'g3 as AnotherGeneric of (String, AnotherGeneric of (X, Y))'
    //var AnotherGenericOfXandY = creator.apply(anotherGenericOfRandS, List.of(x, y));
    //var AnotherGenericOfXandYDash = typeSubstitution.apply(AnotherGenericOfXandY);
    //var g3 = creator.apply(anotherGenericOfRandS, List.of(ek9String, AnotherGenericOfXandYDash));
    //yetAnotherGenericOfXandY.addGenericSymbolReference(typeSubstitution.apply(g3));
  }

  private PossibleGenericSymbol createAnotherGenericOfFloatandString(final PossibleGenericSymbol anotherGenericOfRandS) {
    var parameterizedType = creator.apply(anotherGenericOfRandS, List.of(ek9Float, ek9String));
    var rtn = typeSubstitution.apply(parameterizedType);

    //Now fully concrete!
    assertFalse(rtn.isGenericInNature());
    assertFalse(rtn.isConceptualTypeParameter());
    return rtn;
  }

  private PossibleGenericSymbol createYetAnotherGenericOfDimensionandTime(final PossibleGenericSymbol yetAnotherGenericOfXandY) {
    var parameterizedType = creator.apply(yetAnotherGenericOfXandY, List.of(ek9Dimension, ek9Time));
    var rtn = typeSubstitution.apply(parameterizedType);

    //Now fully concrete!
    assertFalse(rtn.isGenericInNature());
    assertFalse(rtn.isConceptualTypeParameter());
    return rtn;
  }

  /**
   * Just ass the method 'lookup' with parameter type K and V.
   */
  private void addLookupMethod(final PossibleGenericSymbol someGenericOfKandV) {
    var kv = someGenericOfKandV.getTypeParameterOrArguments();

    var k = kv.get(0);
    var v = kv.get(1);
    var arg0 = new VariableSymbol("arg0", k);
    var rtn = new VariableSymbol("rtn", v);
    var someMethod = new MethodSymbol("lookup", someGenericOfKandV);
    someMethod.define(arg0);
    someMethod.setReturningSymbol(rtn);
    someGenericOfKandV.define(someMethod);
  }

  /**
   * Yes more helper functions. this time to create the 'someMethod' with a parameterized someGenericOfKandV.
   */
  private void addSomeMethod(final PossibleGenericSymbol someGenericOfKandV,
                             final PossibleGenericSymbol anotherGenericRandS) {

    //firstly let's make the arg0 we will use as the parameter in the method.
    var newType = createSomeGenericOfRandS(someGenericOfKandV, anotherGenericRandS);
    //So this would be what the compiler would call - but it might be in a difference 'phase'
    var arg0Type = typeSubstitution.apply(newType);

    var arg0 = new VariableSymbol("arg0", arg0Type);
    var someMethod = new MethodSymbol("someMethod", anotherGenericRandS);
    someMethod.define(arg0);
    someMethod.setType(symbolTable.resolve(new TypeSymbolSearch("Void")));
    anotherGenericRandS.define(someMethod);
  }

  private void addAnotherMethod(final PossibleGenericSymbol someGenericOfKandV,
                                final PossibleGenericSymbol anotherGenericRandS) {

    //firstly let's make the arg0 we will use as the parameter in the method.
    var newType = createSomeGenericOfIntegerandS(someGenericOfKandV, anotherGenericRandS);
    //So this would be what the compiler would call - but it might be in a difference 'phase'
    var arg0Type = typeSubstitution.apply(newType);

    var arg0 = new VariableSymbol("arg0", arg0Type);
    var someMethod = new MethodSymbol("anotherMethod", anotherGenericRandS);
    someMethod.define(arg0);
    someMethod.setType(symbolTable.resolve(new TypeSymbolSearch("Void")));
    anotherGenericRandS.define(someMethod);
  }

  private void addConcreteMethod(final PossibleGenericSymbol someGenericOfKandV,
                                 final PossibleGenericSymbol anotherGenericRandS) {
    //firstly let's make the arg0 we will use as the parameter in the method.
    var newType = createSomeGenericOfIntegerandDuration(someGenericOfKandV);
    //So this would be what the compiler would call - but it might be in a difference 'phase'
    var arg0Type = typeSubstitution.apply(newType);

    var arg0 = new VariableSymbol("arg0", arg0Type);
    var someMethod = new MethodSymbol("concreteMethod", anotherGenericRandS);
    someMethod.define(arg0);
    someMethod.setType(symbolTable.resolve(new TypeSymbolSearch("Void")));
    anotherGenericRandS.define(someMethod);
  }

  /**
   * Now we must parameterize someGenericOfKandV with conceptual types 'R' and 'S'.
   * this will result in a 'parameterized type' - but one that is still 'conceptual',
   * meaning it must be parameterized again to be of any actual use.
   */
  private PossibleGenericSymbol createSomeGenericOfRandS(final PossibleGenericSymbol someGenericOfKandV,
                                                         final PossibleGenericSymbol anotherGenericRandS) {
    //first get the 'R' and 'S' we are to use.
    var rs = anotherGenericRandS.getTypeParameterOrArguments();
    var created = creator.apply(someGenericOfKandV, rs);
    assertTrue(created.isGenericInNature());
    //So this means that it is a dependent type.
    /*
    if (created.isGenericInNature()) {
      anotherGenericRandS.addGenericSymbolReference(created);
    }
    */
    return created;
  }

  private PossibleGenericSymbol createSomeGenericOfIntegerandS(final PossibleGenericSymbol someGenericOfKandV,
                                                               final PossibleGenericSymbol anotherGenericRandS) {
    //These are the steps the compiler will take when creating a parameterised type.

    //first get the 'S' we are to use.
    var rs = anotherGenericRandS.getTypeParameterOrArguments();
    //We know that 's' will be the second in the list - ordering is critical in generic type parameters/argument.
    var s = rs.get(1);
    var integerType = symbolTable.resolve(new TypeSymbolSearch("Integer"));
    assertTrue(integerType.isPresent());
    var created = creator.apply(someGenericOfKandV, List.of(integerType.get(), s));
    //So a mix of concrete and conceptual types means this should still be conceptual.
    assertTrue(created.isGenericInNature());

    //So this means that it is a dependent type.
    /*
    if (created.isGenericInNature()) {
      anotherGenericRandS.addGenericSymbolReference(created);
    }
     */

    return created;
  }

  private PossibleGenericSymbol createSomeGenericOfIntegerandDuration(final PossibleGenericSymbol someGenericOfKandV) {
    var integerType = symbolTable.resolve(new TypeSymbolSearch("Integer"));
    assertTrue(integerType.isPresent());
    var durationType = symbolTable.resolve(new TypeSymbolSearch("Duration"));
    assertTrue(durationType.isPresent());
    var created = creator.apply(someGenericOfKandV, List.of(integerType.get(), durationType.get()));
    //Now fully concrete!
    assertFalse(created.isGenericInNature());

    return created;
  }
}

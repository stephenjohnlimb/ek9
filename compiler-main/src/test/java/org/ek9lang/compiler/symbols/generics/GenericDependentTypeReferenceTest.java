package org.ek9lang.compiler.symbols.generics;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.ek9lang.compiler.ParametricResolveOrDefine;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.search.TypeSymbolSearch;
import org.ek9lang.compiler.support.InternalNameFor;
import org.ek9lang.compiler.support.ParameterizedSymbolCreator;
import org.ek9lang.compiler.support.TypeSubstitution;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.symbols.PossibleGenericSymbol;
import org.ek9lang.compiler.symbols.SymbolCategory;
import org.ek9lang.compiler.symbols.VariableSymbol;
import org.ek9lang.compiler.symbols.base.AbstractSymbolTestBase;
import org.ek9lang.compiler.tokenizer.Ek9Token;
import org.junit.jupiter.api.Assertions;
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
 * So we'd expect (in terms of concrete types):
 * <pre>
 *   AnotherGeneric of (Float, String)
 *     SomeGeneric of (Integer, Duration)
 *     SomeGeneric of (Float, String)
 *     SomeGeneric of (Integer, String)
 *   YetAnotherGeneric of (Dimension, Time)
 * </pre>
 * Note that only the types using in parameters in or out are created at this point.
 */
class GenericDependentTypeReferenceTest extends AbstractSymbolTestBase {

  private final ParameterizedSymbolCreator creator = new ParameterizedSymbolCreator(new InternalNameFor());

  private TypeSubstitution typeSubstitution;

  /**
   * This mirrors the ek9 sample source code above and checks it can all be wired together.
   */
  @Test
  void testDependentGenericTypes() {
    //This just simulates the CompilableProgram.
    var parametricResolveOrDefine = new ParametricResolveOrDefine(symbolTable);

    ErrorListener errorListener = new ErrorListener("test");
    typeSubstitution = new TypeSubstitution(parametricResolveOrDefine, errorListener);

    //make the generics outline in the ek9 source snip above.
    var someGenericOfKandV = createSomeGenericOfKandV();
    assertNotNull(someGenericOfKandV);

    var anotherGenericOfRandS = createAnotherGenericOfRandS(someGenericOfKandV);
    assertNotNull(anotherGenericOfRandS);
    //There should be three methods.
    Assertions.assertEquals(3, anotherGenericOfRandS.getSymbolsForThisScope().size());

    //Now need to make 'YetAnotherGeneric of type (X, Y)'
    var yetAnotherGenericOfXandY = createYetAnotherGenericOfXandY();
    assertNotNull(yetAnotherGenericOfXandY);
    Assertions.assertTrue(yetAnotherGenericOfXandY.isGenericInNature());
    Assertions.assertTrue(yetAnotherGenericOfXandY.isConceptualTypeParameter());
    Assertions.assertEquals(1, yetAnotherGenericOfXandY.getSymbolsForThisScope().size());

    //Should/Could have references to:
    //'AnotherGeneric of (Date, Y)'
    //'AnotherGeneric of (X, Boolean)'
    //'AnotherGeneric of (String, AnotherGeneric of (X, Y))'
    //assertEquals(3, YetAnotherGenericOfXandY.getGenericSymbolReferences().size());

    //Now lets make the concrete version using: AnotherGeneric of (Float, String)
    var anotherGenericOfFloatandString = createAnotherGenericOfFloatAndString(anotherGenericOfRandS);
    assertNotNull(anotherGenericOfFloatandString);
    Assertions.assertFalse(anotherGenericOfFloatandString.isGenericInNature());
    //We'd also expect 3 methods with the right types in there
    Assertions.assertEquals(3, anotherGenericOfFloatandString.getSymbolsForThisScope().size());

    //Finally need to make 'yetAnotherGenericOfDimensionAndTime as YetAnotherGeneric of (Dimension, Time)'
    var yetAnotherGenericOfDimensionandTime = createYetAnotherGenericOfDimensionandTime(yetAnotherGenericOfXandY);
    assertNotNull(yetAnotherGenericOfDimensionandTime);
    assertNotNull(yetAnotherGenericOfDimensionandTime);
    Assertions.assertFalse(yetAnotherGenericOfDimensionandTime.isGenericInNature());
    //We'd also expect 1 method with the right types in there
    Assertions.assertEquals(1, yetAnotherGenericOfDimensionandTime.getSymbolsForThisScope().size());

    //Conceptual types
    assertResolution("SomeGeneric", SymbolCategory.TEMPLATE_TYPE);
    assertResolution("AnotherGeneric", SymbolCategory.TEMPLATE_TYPE);
    assertResolution("YetAnotherGeneric", SymbolCategory.TEMPLATE_TYPE);

    //Concrete types
    assertResolution("AnotherGeneric of (Float, String)", SymbolCategory.TYPE);
    assertResolution("YetAnotherGeneric of (Dimension, Time)", SymbolCategory.TYPE);

    assertResolution("SomeGeneric of (Integer, String)", SymbolCategory.TYPE);
    assertResolution("SomeGeneric of (Integer, Duration)", SymbolCategory.TYPE);
    assertResolution("SomeGeneric of (Float, String)", SymbolCategory.TYPE);
  }

  /**
   * Helper method to provide the 'SomeGeneric type of (K, V)'.
   * See the conceptual example at the top of this file.
   * <pre>
   * SomeGeneric of type (K, V)
   *   //Just add one method on this.
   *   lookup()
   *     -> arg0 as K
   *     <- rtn as V
   * </pre>
   */
  private PossibleGenericSymbol createSomeGenericOfKandV() {
    var someGenericOfKandV = new AggregateSymbol("SomeGeneric", symbolTable);
    var k = aggregateManipulator.createGenericT("K", someGenericOfKandV.getFullyQualifiedName(), symbolTable);
    var v = aggregateManipulator.createGenericT("V", someGenericOfKandV.getFullyQualifiedName(), symbolTable);

    someGenericOfKandV.setSourceToken(new Ek9Token());
    someGenericOfKandV.setModuleScope(symbolTable);
    someGenericOfKandV.addTypeParameterOrArgument(k);
    someGenericOfKandV.addTypeParameterOrArgument(v);

    assertTrue(someGenericOfKandV.isGenericInNature());
    assertTrue(someGenericOfKandV.isConceptualTypeParameter());

    addLookupMethod(someGenericOfKandV);
    symbolTable.define(someGenericOfKandV);
    return someGenericOfKandV;
  }

  /**
   * Helper method to provide 'AnotherGeneric type of (R, S)'.
   * This needs the someGenericOfKandV type to be able to populate the methods it defines.
   * <pre>
   * AnotherGeneric of type (R, S)
   *    someMethod()
   *    //So this below is a parameterised type with whatever R and S are going to be
   *    //But it is still 'conceptual' - hence a reference to SomeGeneric is needed
   *      -> arg0 as SomeGeneric of (R, S)
   *    anotherMethod()
   *    //This is still conceptual because it uses S - hence a reference to SomeGeneric is needed
   *      -> arg0 as SomeGeneric of (Integer, S)
   *    concreteMethod()
   *    //But this results in something that can be parameterised and does NOT depend on R or S
   *      -> arg0 as SomeGeneric of (Integer, Duration)
   * </pre>
   */
  private PossibleGenericSymbol createAnotherGenericOfRandS(final PossibleGenericSymbol someGenericOfKandV) {
    var anotherGenericRandS = new AggregateSymbol("AnotherGeneric", symbolTable);
    //So starts out same as above - I've done this 'long hand' so it's quite obvious.
    var r = aggregateManipulator.createGenericT("R", anotherGenericRandS.getFullyQualifiedName(), symbolTable);
    var s = aggregateManipulator.createGenericT("S", anotherGenericRandS.getFullyQualifiedName(), symbolTable);

    anotherGenericRandS.setModuleScope(symbolTable);
    anotherGenericRandS.addTypeParameterOrArgument(r);
    anotherGenericRandS.addTypeParameterOrArgument(s);

    assertTrue(someGenericOfKandV.isGenericInNature());
    assertTrue(someGenericOfKandV.isConceptualTypeParameter());

    addSomeMethod(someGenericOfKandV, anotherGenericRandS);
    addAnotherMethod(someGenericOfKandV, anotherGenericRandS);
    addConcreteMethod(someGenericOfKandV, anotherGenericRandS);

    symbolTable.define(anotherGenericRandS);
    return anotherGenericRandS;
  }

  private PossibleGenericSymbol createYetAnotherGenericOfXandY() {
    //Again here we go - this is what the compiler will do for - but we need to ensure that everything gets wired up
    var yetAnotherGenericOfXandY = new AggregateSymbol("YetAnotherGeneric", symbolTable);
    var x = aggregateManipulator.createGenericT("X", yetAnotherGenericOfXandY.getFullyQualifiedName(), symbolTable);
    var y = aggregateManipulator.createGenericT("Y", yetAnotherGenericOfXandY.getFullyQualifiedName(), symbolTable);

    yetAnotherGenericOfXandY.setModuleScope(symbolTable);
    yetAnotherGenericOfXandY.addTypeParameterOrArgument(x);
    yetAnotherGenericOfXandY.addTypeParameterOrArgument(y);

    assertTrue(yetAnotherGenericOfXandY.isGenericInNature());
    assertTrue(yetAnotherGenericOfXandY.isConceptualTypeParameter());

    addYetAnotherMethod(yetAnotherGenericOfXandY);
    symbolTable.define(yetAnotherGenericOfXandY);
    return yetAnotherGenericOfXandY;
  }

  /**
   * Add a method with no parameters - this is where we simulate adding references.
   * The point being the input parameters and return parameters are 'visible' on the symbol signatures.
   * But code that is used within the body is hidden (implementation detail). Ideally we'd like to create these
   * types as early as possible. This will enable the compiler to detect 'missing operators' when instantiating
   * generics. As we make the assumption in generic code that 'all operators' are available.
   */
  private void addYetAnotherMethod(PossibleGenericSymbol yetAnotherGenericOfXandY) {
    var yetAnotherMethod = new MethodSymbol("yetAnotherMethod", yetAnotherGenericOfXandY);
    yetAnotherMethod.setType(ek9Void);
    yetAnotherGenericOfXandY.define(yetAnotherMethod);
  }

  private PossibleGenericSymbol createAnotherGenericOfFloatAndString(
      final PossibleGenericSymbol anotherGenericOfRandS) {
    var parameterizedType = creator.apply(anotherGenericOfRandS, List.of(ek9Float, ek9String));
    var rtn = typeSubstitution.apply(parameterizedType);

    //Now fully concrete!
    assertFalse(rtn.isGenericInNature());
    assertFalse(rtn.isConceptualTypeParameter());
    return rtn;
  }

  private PossibleGenericSymbol createYetAnotherGenericOfDimensionandTime(
      final PossibleGenericSymbol yetAnotherGenericOfXandY) {
    var parameterizedType = creator.apply(yetAnotherGenericOfXandY, List.of(ek9Dimension, ek9Time));
    var rtn = typeSubstitution.apply(parameterizedType);

    //Now fully concrete!
    assertFalse(rtn.isGenericInNature());
    assertFalse(rtn.isConceptualTypeParameter());
    return rtn;
  }

  /**
   * Just add the method 'lookup' with parameter type K and V.
   */
  private void addLookupMethod(final PossibleGenericSymbol someGenericOfKandV) {
    var kv = someGenericOfKandV.getTypeParameterOrArguments();

    var k = kv.get(0);
    var v = kv.get(1);
    var arg0 = new VariableSymbol("arg0", k);
    var rtn = new VariableSymbol("rtn", v);
    var someMethod = new MethodSymbol("lookup", someGenericOfKandV);
    someMethod.setSourceToken(new Ek9Token());
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
    var newType = createSomeGenericOfIntegerAndS(someGenericOfKandV, anotherGenericRandS);
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

    return created;
  }

  private PossibleGenericSymbol createSomeGenericOfIntegerAndS(final PossibleGenericSymbol someGenericOfKandV,
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

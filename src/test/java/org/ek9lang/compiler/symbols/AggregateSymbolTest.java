package org.ek9lang.compiler.symbols;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.ek9lang.compiler.search.AnyTypeSymbolSearch;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.search.MethodSymbolSearchResult;
import org.ek9lang.compiler.search.SymbolSearch;
import org.ek9lang.compiler.support.AggregateFactory;
import org.ek9lang.compiler.support.SymbolNameOrFail;
import org.ek9lang.compiler.support.SymbolTypeNameOrFail;
import org.junit.jupiter.api.Test;

/**
 * Designed to test a range of aggregate class constructs, such as EK9
 * classes, traits, components - all use the AggregateSymbol
 * This is a major test as it tests Symbol tables, variable symbols and method symbols
 */
final class AggregateSymbolTest {

  final SymbolTypeNameOrFail symbolTypeNameOrFail = new SymbolTypeNameOrFail();
  final SymbolNameOrFail symbolNameOrFail = new SymbolNameOrFail();

  @Test
  void testEmptyAggregate() {
    SymbolTable symbolTable1 = new SymbolTable();

    AggregateSymbol underTest = new AggregateSymbol("UnderTest", symbolTable1);
    assertEquals("UnderTest", underTest.getAggregateDescription());

    var clone = underTest.clone(symbolTable1);
    assertEquals(underTest.hashCode(), clone.hashCode());
    assertEquals(underTest, clone);

    //Check against self.
    assertEquals(underTest, underTest);

    //There are no traits in this or any supers - see AggregateWithTraitsTest for that testing.
    assertEquals(0, underTest.getAllTraits().size());

    SymbolTable symbolTable2 = new SymbolTable();

    //Now make a copy/clone of 'underTest' before setting any values in 'underTest'
    AggregateSymbol cloned1 = underTest.clone(symbolTable2);


    //Alter name to check cloned version is truly cloned.
    //Also check that the type has changed
    underTest.setName("Modified");
    //Just set a range of flags positive and check they stick and cloned version does not have them
    underTest.setAggregateDescription("SomeModifiedAggregate");
    underTest.setMarkedAbstract(true);
    underTest.setInjectable(true);
    underTest.setMarkedAsDispatcher(true);
    underTest.setOpenForExtension(true);
    underTest.setPipeSinkType(Optional.of("ASinkType"));
    underTest.setPipeSourceType(Optional.of("ASourceType"));

    assertTrue(underTest.getTraits().isEmpty());
    assertTrue(underTest.getAllExtensionConstrainedTraits().isEmpty());
    assertFalse((underTest.isExtensionConstrained()));

    //Check all that stuck.
    assertCommonPropertiesAfterSetting(underTest);
    assertEquals("Modified", underTest.getName());
    assertEquals("Modified", symbolNameOrFail.apply(underTest.getType()));

    //Check that underTest and the original clone now differ.
    assertNotEquals(underTest, clone);

    //Check Original clone has not been affected.
    assertCommonPropertiesWithoutSetting(cloned1);

    //Now make another clone with values set.
    SymbolTable symbolTable3 = new SymbolTable();
    AggregateSymbol cloned2 = underTest.clone(symbolTable3);

    assertEquals("UnderTest", cloned1.getName());
    assertEquals("UnderTest", symbolNameOrFail.apply(cloned1.getType()));

    assertEquals("Modified", cloned2.getName());
    assertEquals("Modified", symbolNameOrFail.apply(cloned2.getType()));
    assertCommonPropertiesAfterSetting(cloned2);
  }

  /**
   * Focussed on the checking of methods and method in super types.
   * Plus stuff marked as constructors or abstract.
   */
  @Test
  void testAggregateMethods() {
    SymbolTable symbolTable = new SymbolTable();

    AggregateSymbol base1 = new AggregateSymbol("base1", symbolTable);
    //This is normally used with a 'Component' type construct - which an aggregate can be.
    base1.setInjectable(true);
    //We don't expect any constructors as none have been added
    assertEquals(0, base1.getConstructors().size());

    //Stick with java/C++ naming convention of using the same name.
    //But we actually have to mark it as a constructor method - so it could vary if we wished.
    var constructor1 = new MethodSymbol("base1", base1);
    constructor1.setConstructor(true);
    base1.define(constructor1);
    assertEquals(1, base1.getConstructors().size());
    assertEquals(constructor1, base1.getConstructors().get(0));

    var abstractMethod1 = new MethodSymbol("abstractMethod", base1);
    abstractMethod1.setMarkedAbstract(true);
    base1.define(abstractMethod1);

    //There should still only be the constructor
    assertEquals(1, base1.getAllNonAbstractMethods().size());
    assertEquals(constructor1, base1.getAllNonAbstractMethods().get(0));
    //And one abstract method
    assertEquals(1, base1.getAllAbstractMethods().size());
    assertEquals(abstractMethod1, base1.getAllAbstractMethods().get(0));

    //Now lets make a sub class and try the method access
    AggregateSymbol cls1 = new AggregateSymbol("cls1", symbolTable);
    assertFalse(cls1.isExtensionOfInjectable());

    cls1.setSuperAggregate(base1);
    assertTrue(cls1.isExtensionOfInjectable());

    //Now add another constructor - but for cls1 this time.
    //make sure we only find that constructor.
    var constructor2 = new MethodSymbol("cls1", base1);
    constructor2.setConstructor(true);
    cls1.define(constructor2);
    assertEquals(1, cls1.getConstructors().size());
    assertEquals(constructor2, cls1.getConstructors().get(0));

    //But if we do want 'all methods that are not abstract'
    assertEquals(2, cls1.getAllNonAbstractMethods().size());
    assertEquals(constructor1, cls1.getAllNonAbstractMethods().get(0));
    assertEquals(constructor2, cls1.getAllNonAbstractMethods().get(1));

    //Now abstract method on cls1
    var abstractMethod2 = new MethodSymbol("abstractMethodAlpha", cls1);
    abstractMethod2.setMarkedAbstract(true);
    cls1.define(abstractMethod2);

    assertEquals(2, cls1.getAllAbstractMethods().size());
    assertEquals(abstractMethod1, cls1.getAllAbstractMethods().get(0));
    assertEquals(abstractMethod2, cls1.getAllAbstractMethods().get(1));

    //Now lets also 'implement' that abstract method in the base.
    var concreteMethod = new MethodSymbol("abstractMethod", cls1);
    concreteMethod.setMarkedAbstract(false);
    cls1.define(concreteMethod);

    //Limit to just cls1 and our 'implemented' method.
    assertEquals(2, cls1.getAllNonAbstractMethodsInThisScopeOnly().size());
    assertEquals(constructor2, cls1.getAllNonAbstractMethodsInThisScopeOnly().get(0));
    assertEquals(concreteMethod, cls1.getAllNonAbstractMethodsInThisScopeOnly().get(1));

    var results = cls1.resolveMatchingMethods(new MethodSymbolSearch("noSuchMethod"), new MethodSymbolSearchResult());
    assertTrue(results.isEmpty());

    //Now lets search for 'abstractMethod'.
    results = cls1.resolveMatchingMethods(new MethodSymbolSearch("abstractMethod"), new MethodSymbolSearchResult());
    var bestMethodMatch = results.getSingleBestMatchSymbol();
    assertTrue(bestMethodMatch.isPresent());
    assertEquals(concreteMethod, bestMethodMatch.get());

    //Now just to be sure nothing somehow made it into base when we were adding to cls1
    assertEquals(1, base1.getConstructors().size());
    assertEquals(constructor1, base1.getConstructors().get(0));
    assertEquals(1, base1.getAllAbstractMethods().size());
    assertEquals(abstractMethod1, base1.getAllAbstractMethods().get(0));

    //But if we did the same on base1
    results = base1.resolveMatchingMethods(new MethodSymbolSearch("abstractMethod"), new MethodSymbolSearchResult());
    bestMethodMatch = results.getSingleBestMatchSymbol();
    assertTrue(bestMethodMatch.isPresent());
    assertEquals(abstractMethod1, bestMethodMatch.get());
  }

  /**
   * This test focuses on extension of a generic type.
   */
  @Test
  void testExtendingAGenericType() {
    SymbolTable symbolTable = new SymbolTable();
    AggregateFactory support = new AggregateFactory();
    var t = support.createGenericT("T", symbolTable);
    var aGenericBaseType = new AggregateSymbol("GenericBase", symbolTable, List.of(t));

    var s = support.createGenericT("S", symbolTable);
    var anotherGenericType = new AggregateSymbol("AnotherGenericBase", symbolTable, List.of(s, t));

    //Check not equal
    assertNotEquals(aGenericBaseType, anotherGenericType);

    //Now make a plain aggregate and extend from the generic base.
    AggregateSymbol cls1 = new AggregateSymbol("cls1", symbolTable);
    cls1.setSuperAggregate(aGenericBaseType);

    //Is it possible to resolve 'T'? Not sure why we would need to but we should be able to.
    var resolvedT = cls1.resolve(new AnyTypeSymbolSearch("T"));
    assertTrue(resolvedT.isPresent());
    assertEquals(t, resolvedT.get());
  }


  @Test
  void testExtendingAParameterisedGenericType() {
    AggregateFactory support = new AggregateFactory();

    SymbolTable symbolTable = new SymbolTable();
    AggregateSymbol integerType = new AggregateSymbol("Integer", symbolTable);
    symbolTable.define(integerType);

    //Make the 'T' and generic type that will use the 'T' as a conceptual type parameter
    var t = support.createGenericT("T", symbolTable);
    var aGenericType = new AggregateSymbol("GenericType", symbolTable, List.of(t));

    //Make the parameterised type using the generic type and parameterize it with concrete type Integer.
    var parameterisedType1 = new AggregateSymbol("ParameterisedWithInteger", symbolTable);
    parameterisedType1.setGenericType(aGenericType);
    parameterisedType1.addTypeParameterOrArgument(integerType);

    //Now make a plain aggregate and extend from the generic base.
    AggregateSymbol cls1 = new AggregateSymbol("cls1", symbolTable);
    cls1.setSuperAggregate(parameterisedType1);

    //Is it possible to resolve 'T', it should NOT be possible as 'T' is hidden within the parameterised type.
    var resolvedT = cls1.resolve(new AnyTypeSymbolSearch("T"));
    assertFalse(resolvedT.isPresent());
  }

  /**
   * Focussed on checking the properties and properties in supers.
   */
  @Test
  void testAggregateProperties() {
    //Make a symbol table and make a type available.
    SymbolTable symbolTable = new SymbolTable();
    AggregateSymbol integerType = new AggregateSymbol("Integer", symbolTable);
    symbolTable.define(integerType);

    var field1 = new VariableSymbol("field1", integerType);
    field1.setAggregatePropertyField(true);
    AggregateSymbol base1 = new AggregateSymbol("base1", symbolTable);
    base1.define(field1);

    assertEquals(1, base1.getProperties().size());
    assertEquals(field1, base1.getProperties().get(0));

    //We can find by the correct type of search like this as well
    var resolvedField = base1.resolveMember(new SymbolSearch("field1"));
    assertTrue(resolvedField.isPresent());
    assertEquals(field1, resolvedField.get());

    //Does not resolve when asking for methods.
    var notResolvedField = base1.resolveMember(new MethodSymbolSearch("field1"));
    assertTrue(notResolvedField.isEmpty());

    //Lets add another field and see if can be located by resolveMember
    var field2 = new VariableSymbol("field2", integerType);
    field2.setAggregatePropertyField(true);
    base1.define(field2);
    assertEquals(2, base1.getProperties().size());

    //Now make a sub class of base1.
    AggregateSymbol cls1 = new AggregateSymbol("cls1", symbolTable);
    cls1.setSuperAggregate(base1);
    //Note that this does not pull out properties from super!
    assertEquals(0, cls1.getProperties().size());

    //Now lets add the same field name again but in this sub class - all should be OK
    var alsoField1 = new VariableSymbol("field1", integerType);
    alsoField1.setAggregatePropertyField(true);
    cls1.define(alsoField1);
    assertEquals(1, cls1.getProperties().size());
    assertEquals(alsoField1, cls1.getProperties().get(0));

    //Let's see if we can resolve field2 through the class hierarchy
    var resolvedField2 = cls1.resolveMember(new SymbolSearch("field2"));
    assertTrue(resolvedField2.isPresent());
    assertEquals(field2, resolvedField2.get());
  }

  @Test
  void testAggregateWithSuper() {
    SymbolTable symbolTable1 = new SymbolTable();

    AggregateSymbol base1 = new AggregateSymbol("base1", symbolTable1);

    var self = base1.getType();
    assertTrue(self.isPresent());
    assertEquals(base1, self.get());

    AggregateSymbol cls1 = new AggregateSymbol("cls1", symbolTable1);

    assertFalse(cls1.isImplementingInSomeWay(base1));
    assertFalse(cls1.hasImmediateSuper(base1));

    //This should have two effects, setting the super in cls1 and adding a sub to base1
    cls1.setSuperAggregate(base1);

    assertTrue(cls1.isImplementingInSomeWay(base1));
    assertTrue(cls1.hasImmediateSuper(base1));

    var theSuper = cls1.getAnySuperTypeOrFunction();
    assertTrue(theSuper.isPresent());
    assertEquals(base1, theSuper.get());

    //Now check the base has cls1 as a sub type
    var subs = base1.getSubAggregateSymbols();
    assertEquals(1, subs.size());
    assertEquals(cls1, subs.get(0));

    //Let's check assignability
    assertTrue(cls1.isAssignableTo(cls1));
    assertTrue(cls1.isAssignableTo(base1));
    //But not the opposite
    assertFalse(base1.isAssignableTo(cls1));

    //Now clone
    var cloned = cls1.clone(symbolTable1);
    assertEquals(cls1, cloned);
    cloned.setName("cloned");
    assertNotEquals(cls1, cloned);

    //Now lets add another sub of the base
    AggregateSymbol cls2 = new AggregateSymbol("cls2", symbolTable1);
    cls2.setSuperAggregate(base1);
    theSuper = cls2.getAnySuperTypeOrFunction();
    assertTrue(theSuper.isPresent());
    assertEquals(base1, theSuper.get());

    subs = base1.getSubAggregateSymbols();
    assertEquals(3, subs.size());
    assertEquals(cls1, subs.get(0));
    assertEquals(cloned, subs.get(1));
    assertEquals(cls2, subs.get(2));

    //Let's check assignability for cls2 and check cls1 and cls2 are not assignable.
    assertTrue(cls2.isAssignableTo(base1));
    //But not the opposite
    assertFalse(base1.isAssignableTo(cls2));

    assertFalse(cls1.isAssignableTo(cls2));
    assertFalse(cls2.isAssignableTo(cls1));

  }

  private void assertCommonPropertiesWithoutSetting(AggregateSymbol toCheck) {
    assertNotNull(toCheck);
    assertFalse(toCheck.isMarkedAbstract());
    assertFalse(toCheck.isInjectable());
    assertFalse(toCheck.isMarkedAsDispatcher());
    assertFalse(toCheck.isOpenForExtension());
    assertFalse(toCheck.getPipeSinkType().isPresent());
    assertFalse(toCheck.getPipeSourceType().isPresent());
  }

  private void assertCommonPropertiesAfterSetting(AggregateSymbol toCheck) {
    assertNotNull(toCheck);
    assertTrue(toCheck.isMarkedAbstract());
    assertTrue(toCheck.isInjectable());
    assertTrue(toCheck.isMarkedAsDispatcher());
    assertTrue(toCheck.isOpenForExtension());
    assertTrue(toCheck.getPipeSinkType().isPresent());
    assertEquals("ASinkType", toCheck.getPipeSinkType().get());
    assertTrue(toCheck.getPipeSourceType().isPresent());
    assertEquals("ASourceType", toCheck.getPipeSourceType().get());
  }

  @Test
  void testAggregateMethod() {
    IScope symbolTable = new SymbolTable();

    AggregateSymbol integerType = new AggregateSymbol("Integer", symbolTable);
    symbolTable.define(integerType);
    AggregateSymbol stringType = new AggregateSymbol("String", symbolTable);
    symbolTable.define(stringType);

    AggregateSymbol underTest = new AggregateSymbol("UnderTest", symbolTable);

    MethodSymbol method = new MethodSymbol("method1", integerType, underTest);
    method.define(new VariableSymbol("arg1", stringType));
    underTest.define(method);

    assertEquals(1, underTest.getAllNonAbstractMethodsInThisScopeOnly().size());
    assertEquals(method, underTest.getAllNonAbstractMethodsInThisScopeOnly().get(0));

    //Now search and find that method, must add in the param to be able to resolve it.
    Optional<ISymbol> resolvedMethod = underTest.resolveInThisScopeOnly(
        new MethodSymbolSearch("method1").addTypeParameter(new VariableSymbol("arg1", stringType)));
    assertTrue(resolvedMethod.isPresent());
    assertSymbol(resolvedMethod, "method1", "Integer");

    //Now clone and check again
    AggregateSymbol cloned = underTest.clone(symbolTable);
    assertNotNull(cloned);
    assertEquals(underTest, cloned);

    resolvedMethod = cloned.resolveInThisScopeOnly(
        new MethodSymbolSearch("method1").addTypeParameter(new VariableSymbol("arg1", stringType)));
    assertTrue(resolvedMethod.isPresent());
    assertSymbol(resolvedMethod, "method1", "Integer");

    //Now alter method1 cloned version but original should stay as is.
    MethodSymbol toAlter = (MethodSymbol) resolvedMethod.get();
    toAlter.setType(stringType);
    toAlter.define(new VariableSymbol("arg2", integerType));

    //Check now not equal
    assertNotEquals(underTest, cloned);

    //Test original is still OK
    resolvedMethod = underTest.resolveInThisScopeOnly(
        new MethodSymbolSearch("method1").addTypeParameter(new VariableSymbol("arg1", stringType)));
    assertTrue(resolvedMethod.isPresent());
    assertSymbol(resolvedMethod, "method1", "Integer");

    //Now check cloned one has been altered
    resolvedMethod = cloned.resolveInThisScopeOnly(
        new MethodSymbolSearch("method1").addTypeParameter(new VariableSymbol("arg1", stringType)));
    assertFalse(resolvedMethod.isPresent());

    resolvedMethod = cloned.resolveInThisScopeOnly(
        new MethodSymbolSearch("method1")
            .addTypeParameter(new VariableSymbol("arg1", stringType))
            .addTypeParameter(new VariableSymbol("arg2", integerType)));
    assertTrue(resolvedMethod.isPresent());
    assertSymbol(resolvedMethod, "method1", "String");

  }

  @Test
  void testAggregateProperty() {
    IScope symbolTable = new SymbolTable();

    AggregateSymbol integerType = new AggregateSymbol("Integer", symbolTable);
    symbolTable.define(integerType);
    AggregateSymbol stringType = new AggregateSymbol("String", symbolTable);
    symbolTable.define(stringType);

    AggregateSymbol underTest = new AggregateSymbol("UnderTest", symbolTable);
    assertEquals("UnderTest", underTest.getAggregateDescription());

    VariableSymbol prop1 = new VariableSymbol("prop1", integerType);
    underTest.define(prop1);

    assertEquals(1, underTest.getProperties().size());
    assertEquals(prop1, underTest.getProperties().get(0));

    Optional<ISymbol> resolvedProperty =
        underTest.resolveInThisScopeOnly(new SymbolSearch("prop1"));
    assertSymbol(resolvedProperty, "prop1", "Integer");

    AggregateSymbol cloned = underTest.clone(symbolTable);
    assertNotNull(cloned);

    //Now check same can be found on the cloned version
    resolvedProperty = cloned.resolveInThisScopeOnly(new SymbolSearch("prop1"));
    assertSymbol(resolvedProperty, "prop1", "Integer");

    //Now alter prop1 type on the cloned version but original should stay as is.
    resolvedProperty.ifPresent(prop -> prop.setType(stringType));

    //Search and find again to check.
    resolvedProperty = underTest.resolveInThisScopeOnly(new SymbolSearch("prop1"));
    assertSymbol(resolvedProperty, "prop1", "Integer");

    //But cloned should be String
    resolvedProperty = cloned.resolveInThisScopeOnly(new SymbolSearch("prop1"));
    assertSymbol(resolvedProperty, "prop1", "String");
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private void assertSymbol(Optional<ISymbol> resolvedSymbol, String expectedName,
                            String expectedTypeName) {
    assertTrue(resolvedSymbol.isPresent());
    assertEquals(expectedName, resolvedSymbol.get().getName());
    assertEquals(expectedTypeName, symbolTypeNameOrFail.apply(resolvedSymbol));
  }
}

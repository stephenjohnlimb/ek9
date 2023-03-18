package org.ek9lang.compiler.symbol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.ek9lang.compiler.symbol.support.SymbolNameOrFail;
import org.ek9lang.compiler.symbol.support.SymbolTypeNameOrFail;
import org.ek9lang.compiler.symbol.support.search.MethodSymbolSearch;
import org.ek9lang.compiler.symbol.support.search.SymbolSearch;
import org.junit.jupiter.api.Test;

/**
 * Designed to test a range of aggregate class constructs, such as EK9
 * classes, traits, components - all use the AggregateSymbol
 * <p>
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

    //Check all that stuck.
    assertCommonPropertiesAfterSetting(underTest);
    assertEquals("Modified", underTest.getName());
    assertEquals("Modified", symbolNameOrFail.apply(underTest.getType()));

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

    //Now search and find that method, must add in the param to be able to resolve it.
    Optional<ISymbol> resolvedMethod = underTest.resolveInThisScopeOnly(
        new MethodSymbolSearch("method1").addParameter(new VariableSymbol("arg1", stringType)));
    assertTrue(resolvedMethod.isPresent());
    assertSymbol(resolvedMethod, "method1", "Integer");

    //Now clone and check again
    AggregateSymbol cloned = underTest.clone(symbolTable);
    assertNotNull(cloned);
    resolvedMethod = cloned.resolveInThisScopeOnly(
        new MethodSymbolSearch("method1").addParameter(new VariableSymbol("arg1", stringType)));
    assertTrue(resolvedMethod.isPresent());
    assertSymbol(resolvedMethod, "method1", "Integer");

    //Now alter method1 cloned version but original should stay as is.
    MethodSymbol toAlter = (MethodSymbol) resolvedMethod.get();
    toAlter.setType(stringType);
    toAlter.define(new VariableSymbol("arg2", integerType));

    //Test original is still OK
    resolvedMethod = underTest.resolveInThisScopeOnly(
        new MethodSymbolSearch("method1").addParameter(new VariableSymbol("arg1", stringType)));
    assertTrue(resolvedMethod.isPresent());
    assertSymbol(resolvedMethod, "method1", "Integer");

    //Now check cloned one has been altered
    resolvedMethod = cloned.resolveInThisScopeOnly(
        new MethodSymbolSearch("method1").addParameter(new VariableSymbol("arg1", stringType)));
    assertFalse(resolvedMethod.isPresent());

    resolvedMethod = cloned.resolveInThisScopeOnly(
        new MethodSymbolSearch("method1")
            .addParameter(new VariableSymbol("arg1", stringType))
            .addParameter(new VariableSymbol("arg2", integerType)));
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

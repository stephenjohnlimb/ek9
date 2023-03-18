package org.ek9lang.compiler.symbol;

import static org.ek9lang.compiler.symbol.support.AggregateFactory.EK9_INTEGER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.symbol.support.AggregateFactory;
import org.ek9lang.compiler.symbol.support.TypeCreator;
import org.ek9lang.compiler.symbol.support.search.MethodSymbolSearch;
import org.ek9lang.compiler.symbol.support.search.MethodSymbolSearchResult;
import org.ek9lang.compiler.symbol.support.search.SymbolSearch;
import org.ek9lang.compiler.symbol.support.search.TypeSymbolSearch;
import org.ek9lang.core.utils.Logger;
import org.junit.jupiter.api.Test;

/**
 * Aimed at testing scopes and in some cases scoped symbols.
 * <p>
 * Does not fully test aggregates and the like, just their scoped natures.
 */
final class ScopesTest extends AbstractSymbolTestBase {

  private final TypeCreator typeCreator = new TypeCreator();

  /**
   * Need to test for symbols declared in a module fully qualified and also non fully qualified searches.
   */
  @Test
  void testModuleNamedSymbolResolution() {
    //Raw none-module named
    var module1SymbolTable = new SymbolTable("");

    //This would be a raw String i.e. not in a moduleScope - so not a true reflection of what we will be doing
    //Hence even fully qualified it will just be String
    ISymbol stringType = typeCreator.apply("String", module1SymbolTable);
    assertEquals(stringType.getName(), stringType.getFullyQualifiedName());

    ISymbol noModuleInteger = typeCreator.apply("Integer", module1SymbolTable);
    assertEquals(noModuleInteger.getName(), noModuleInteger.getFullyQualifiedName());

    //But this Integer is going to be in an actual module so can be fully qualified
    var ek9LangSymbolTable = new SymbolTable(AggregateFactory.EK9_LANG);
    ISymbol orgEk9LangInteger = typeCreator.apply("Integer", ek9LangSymbolTable);

    assertEquals("Integer", orgEk9LangInteger.getName());
    assertEquals(EK9_INTEGER, orgEk9LangInteger.getFullyQualifiedName());


    //Now check that the noModuleInteger is not considered to be the same.
    assertFalse(noModuleInteger.isExactSameType(orgEk9LangInteger));
    //and the other way.
    assertFalse(orgEk9LangInteger.isExactSameType(noModuleInteger));

    //Search in the no module symbol table
    assertTrue(module1SymbolTable.resolve(new TypeSymbolSearch("Integer")).isPresent());
    assertFalse(module1SymbolTable.resolve(new TypeSymbolSearch(EK9_INTEGER)).isPresent());
  }

  @Test
  void testSameSymbolsInModuleResolve() {
    var ek9LangSymbolTable = new SymbolTable(AggregateFactory.EK9_LANG);
    ISymbol orgEk9LangInteger = typeCreator.apply("Integer", ek9LangSymbolTable);

    var anotherEk9LangSymbolTable = new SymbolTable(AggregateFactory.EK9_LANG);
    ISymbol anotherOrgEk9LangInteger = typeCreator.apply("Integer", anotherEk9LangSymbolTable);

    assertTrue(anotherOrgEk9LangInteger.isExactSameType(orgEk9LangInteger));
    //And the other way just to be sure
    assertTrue(orgEk9LangInteger.isExactSameType(anotherOrgEk9LangInteger));
  }

  @Test
  void testFullyQualifiedSearch() {
    var ek9LangSymbolTable = new SymbolTable(AggregateFactory.EK9_LANG);
    ISymbol orgEk9LangInteger = typeCreator.apply("Integer", ek9LangSymbolTable);
    assertNotNull(orgEk9LangInteger);
    assertFalse(ek9LangSymbolTable.resolve(new TypeSymbolSearch("Integer")).isPresent());
    assertTrue(ek9LangSymbolTable.resolve(new TypeSymbolSearch(EK9_INTEGER)).isPresent());
  }

  @Test
  void testLocalScope() {
    var local1 = new LocalScope(symbolTable);
    var local2 = new LocalScope(symbolTable);

    //These will be considered equal
    assertEquals(local1, local2);
    assertEquals(local1.hashCode(), local2.hashCode());

    assertNotNull(local1);
    assertEquals(IScope.ScopeType.BLOCK, local1.getScopeType());
    assertFalse(local1.isMarkedPure());
    assertTrue(local1.isScopeAMatchForEnclosingScope(symbolTable));
    assertFalse(local1.isScopeAMatchForEnclosingScope(new SymbolTable()));
  }

  @Test
  void testDifferentLocalScopes() {
    //Normally when creating local scopes, we could use the name of the file and line no.
    //as the unique identifier of the local scope.
    var local1 = new LocalScope("l1", symbolTable);
    var local2 = new LocalScope("l2", symbolTable);

    assertNotEquals(local1, local2);
    assertNotEquals(local1.hashCode(), local2.hashCode());
  }

  @Test
  void testEnclosingScopeSymbolResolution() {
    var local1 = new LocalScope(symbolTable);

    var resolved = local1.resolve(new TypeSymbolSearch("Boolean"));
    assertTrue(resolved.isPresent());
    assertEquals("Boolean", resolved.get().getName());
  }

  @Test
  void testBlockLimitingResolution() {
    var local1 = new LocalScope(IScope.ScopeType.NON_BLOCK, "SimulatedAggregate", symbolTable);
    var local2 = new LocalScope(local1);

    //Now if we limit to blocks, should not be able to resolve.
    var notResolved = local2
        .resolve(new TypeSymbolSearch("Boolean").setLimitToBlocks(true));
    assertFalse(notResolved.isPresent());
  }

  @Test
  void testGeneralMethodResolution() {
    var local1 = new LocalScope(IScope.ScopeType.NON_BLOCK, "SimulatedAggregate", symbolTable);

    //Create a method and add.
    var method = new MethodSymbol("aMethod", local1);
    local1.define(method);

    var local2 = new LocalScope(local1);
    //Now lets see if we can find that method in the simulated aggregate
    var results = new MethodSymbolSearchResult();
    results = local2.resolveMatchingMethods(new MethodSymbolSearch("aMethod"), results);

    assertTrue(results.isSingleBestMatchPresent());
    assertTrue(results.getSingleBestMatchSymbol().isPresent());
    assertEquals(method, results.getSingleBestMatchSymbol().get());
  }

  @Test
  void testNamedScope() {
    //It's not really an aggregate scope but let;s test that type of setting.
    VariableSymbol v1 =
        new VariableSymbol("v3", symbolTable.resolve(new TypeSymbolSearch("Integer")));

    var local = new LocalScope(IScope.ScopeType.NON_BLOCK, "someName", symbolTable);
    assertNotNull(local);
    local.define(v1);
    assertEquals("someName", local.getScopeName());
    assertEquals(IScope.ScopeType.NON_BLOCK, local.getScopeType());
    assertEquals("someName", local.getFriendlyScopeName());

    var clone = local.clone(symbolTable);
    assertNotNull(clone);
    assertEquals(local, clone);
    assertNotEquals("AString", local.getScopeName());
  }

  @Test
  void testFindingAggregateScope() {
    var aggregateScope1 =
        new ScopedSymbol(IScope.ScopeType.NON_BLOCK, "aggregateScope", symbolTable);
    assertFalse(aggregateScope1.isMarkedPure());
    assertEquals("aggregateScope as Unknown", aggregateScope1.getFriendlyScopeName());
    assertEquals("aggregateScope", aggregateScope1.getName());
    assertEquals("aggregateScope", aggregateScope1.getScopeName());
    assertNotNull(aggregateScope1.getActualScope());

    var blockScope1 = new ScopedSymbol(IScope.ScopeType.BLOCK, "blockScope", aggregateScope1);

    var local = new LocalScope("JustLocalBlock", blockScope1);

    var foundScope = local.findNearestNonBlockScopeInEnclosingScopes();
    assertTrue(foundScope.isPresent());
    assertEquals(aggregateScope1, foundScope.get());

    assertTrue(blockScope1.isScopeAMatchForEnclosingScope(aggregateScope1));
  }

  @Test
  void testAggregateSymbolScope() {
    //So this would be an actual 'type' like an OOP 'Customer' for example
    assertNotNull(checkScopedSymbol(new AggregateSymbol("aggregate", symbolTable)));
  }

  @Test
  void testAggregateWithTraitsSymbolScope() {
    //This would be an aggregate that implements a number of traits.
    assertNotNull(
        checkScopedSymbol(new AggregateWithTraitsSymbol("aggregateWithTraits", symbolTable)));
  }

  @Test
  void testParameterisedTypeSymbolScope() {
    var t = support.createGenericT("Tee", symbolTable);
    var z = support.createTemplateGenericType("Zee", symbolTable, t);
    symbolTable.define(z);

    assertEquals("Zee of type Tee", z.getFriendlyName());

    //Check it is possible resolve 'Tee' from within 'Zee'
    var resolvedTee = z.resolve(new TypeSymbolSearch("Tee"));
    assertTrue(resolvedTee.isPresent());

    //This would be a concrete Zee with a concrete type of String to replace 'Tee'
    var pTypeSymbol =
        new ParameterisedTypeSymbol(z, symbolTable.resolve(new TypeSymbolSearch("String")),
            symbolTable);
    var clonedPTypeSymbol = checkScopedSymbol(pTypeSymbol);
    assertNotNull(clonedPTypeSymbol);
    Logger.log(pTypeSymbol.getFriendlyName());
    Logger.log(clonedPTypeSymbol.getFriendlyName());

    assertEquals("Zee of String", pTypeSymbol.getFriendlyName());
  }

  @Test
  void testParameterisedFunctionSymbolScope() {
    var t = support.createGenericT("Tee", symbolTable);
    var fun = support.createTemplateGenericFunction("fun", symbolTable, t);
    symbolTable.define(fun);

    //We've not defined the return type of the function
    assertEquals("Unknown <- fun() of type Tee", fun.getFriendlyName());

    //Check it is possible resolve Tee from within 'fun'
    var resolvedTee = fun.resolve(new TypeSymbolSearch("Tee"));
    assertTrue(resolvedTee.isPresent());

    fun.setReturningSymbol(
        new VariableSymbol("rtn", symbolTable.resolve(new TypeSymbolSearch("Integer"))));
    assertEquals("Integer <- fun() of type Tee", fun.getFriendlyName());

    //This would be a concrete 'fun' with a concrete type of String to replace 'Tee'
    var pFun =
        new ParameterisedFunctionSymbol(fun, symbolTable.resolve(new TypeSymbolSearch("String")),
            symbolTable);

    assertNotNull(pFun.getReturningSymbol());
    var clonedPFun = checkScopedSymbol(pFun);
    assertNotNull(clonedPFun);

    //The return is the function should be the return type.
    Logger.log(pFun.getFriendlyName());

    Logger.log(clonedPFun.getFriendlyName());
  }

  @Test
  void testControlSymbolScope() {
    assertNotNull(checkScopedSymbol(new ControlSymbol("Control", symbolTable)));
  }

  @Test
  void testScopedSymbolScope() {
    assertNotNull(checkScopedSymbol(
        new ScopedSymbol(IScope.ScopeType.NON_BLOCK, "aggregateScope", symbolTable)));
  }

  @Test
  void testForSymbolScope() {
    assertNotNull(checkScopedSymbol(new ForSymbol(symbolTable)));
  }

  @Test
  void testMethodSymbolScope() {
    assertNotNull(checkScopedSymbol(new MethodSymbol("aMethod", symbolTable)));
  }

  @Test
  void testFunctionSymbolScope() {
    assertNotNull(checkScopedSymbol(new FunctionSymbol("aFunction", symbolTable)));
  }

  @Test
  void testCallSymbolScope() {
    assertNotNull(checkScopedSymbol(new CallSymbol("aMethodCall", symbolTable)));
  }

  @Test
  void testServiceOperationSymbolScope() {
    assertNotNull(
        checkScopedSymbol(new ServiceOperationSymbol("aServiceOperationCall", symbolTable)));
  }

  @Test
  void testStreamCallSymbolScope() {
    assertNotNull(checkScopedSymbol(new StreamCallSymbol("aStreamCall", symbolTable)));
  }

  @Test
  void testSwitchSymbolScope() {
    assertNotNull(checkScopedSymbol(new SwitchSymbol(symbolTable)));
  }

  @Test
  void testTrySymbolScope() {
    assertNotNull(checkScopedSymbol(new TrySymbol(symbolTable)));
  }

  private ScopedSymbol checkScopedSymbol(ScopedSymbol scopedSymbol) {
    assertNotNull(scopedSymbol);

    //So all we are doing here - is checking that we can add some sort of 'thing'
    //Then clone the scoped symbol and ensure the 'thing' we added is also in that.
    scopedSymbol.define(
        new VariableSymbol("check", scopedSymbol.resolve(new TypeSymbolSearch("String"))));
    assertTrue(scopedSymbol.resolve(new SymbolSearch("check")).isPresent());

    var clonedSymbol = scopedSymbol.clone(symbolTable);
    assertTrue(clonedSymbol.resolve(new SymbolSearch("check")).isPresent());

    return clonedSymbol;
  }
}

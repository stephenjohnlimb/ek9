package org.ek9lang.compiler.symbol.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.symbol.AggregateSymbol;
import org.ek9lang.compiler.symbol.AggregateWithTraitsSymbol;
import org.ek9lang.compiler.symbol.CallSymbol;
import org.ek9lang.compiler.symbol.ControlSymbol;
import org.ek9lang.compiler.symbol.ForSymbol;
import org.ek9lang.compiler.symbol.FunctionSymbol;
import org.ek9lang.compiler.symbol.IScope;
import org.ek9lang.compiler.symbol.LocalScope;
import org.ek9lang.compiler.symbol.MethodSymbol;
import org.ek9lang.compiler.symbol.ParameterisedFunctionSymbol;
import org.ek9lang.compiler.symbol.ParameterisedTypeSymbol;
import org.ek9lang.compiler.symbol.ScopedSymbol;
import org.ek9lang.compiler.symbol.ServiceOperationSymbol;
import org.ek9lang.compiler.symbol.StreamCallSymbol;
import org.ek9lang.compiler.symbol.SwitchSymbol;
import org.ek9lang.compiler.symbol.TrySymbol;
import org.ek9lang.compiler.symbol.VariableSymbol;
import org.ek9lang.compiler.symbol.support.search.SymbolSearch;
import org.ek9lang.compiler.symbol.support.search.TypeSymbolSearch;
import org.ek9lang.core.utils.Logger;
import org.junit.jupiter.api.Test;

/**
 * Aimed at testing scopes and in some cases scoped symbols.
 * <p>
 * Does not fully test aggregates and the like, just their scoped type natures.
 */
final class ScopesTest extends AbstractSymbolTestBase {
  @Test
  void testLocalScope() {
    var local = new LocalScope(symbolTable);

    assertNotNull(local);
    assertEquals(IScope.ScopeType.BLOCK, local.getScopeType());
    assertFalse(local.isMarkedPure());
    assertTrue(local.isScopeAMatchForEnclosingScope(symbolTable));
    assertFalse(local.isScopeAMatchForEnclosingScope(new SymbolTable()));
  }

  @Test
  void testNamedScope() {
    //It's not really an aggregate scope but let;s test that type of setting.
    VariableSymbol v1 =
        new VariableSymbol("v3", symbolTable.resolve(new TypeSymbolSearch("Integer")));

    var local = new LocalScope(IScope.ScopeType.AGGREGATE, "someName", symbolTable);
    assertNotNull(local);
    local.define(v1);
    assertEquals("someName", local.getScopeName());
    assertEquals(IScope.ScopeType.AGGREGATE, local.getScopeType());
    assertEquals("someName", local.getFriendlyScopeName());

    var clone = local.clone(symbolTable);
    assertNotNull(clone);
    assertEquals(local, clone);
    assertNotEquals("AString", local.getScopeName());
  }

  @Test
  void testFindingAggregateScope() {
    var aggregateScope1 =
        new ScopedSymbol(IScope.ScopeType.AGGREGATE, "aggregateScope", symbolTable);
    assertFalse(aggregateScope1.isMarkedPure());
    assertEquals("aggregateScope as Unknown", aggregateScope1.getFriendlyScopeName());
    assertEquals("aggregateScope", aggregateScope1.getName());
    assertEquals("aggregateScope", aggregateScope1.getScopeName());
    assertNotNull(aggregateScope1.getActualScope());

    var blockScope1 = new ScopedSymbol(IScope.ScopeType.BLOCK, "blockScope", aggregateScope1);

    var local = new LocalScope("JustLocalBlock", blockScope1);

    var foundScope = local.findNearestAggregateScopeInEnclosingScopes();
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
    //This would be a concrete Zee with a concrete type of String to replace 'Tee'
    var pTypeSymbol =
        new ParameterisedTypeSymbol(z, symbolTable.resolve(new TypeSymbolSearch("String")),
            symbolTable);
    assertNotNull(checkScopedSymbol(pTypeSymbol));

    assertEquals("Zee of String", pTypeSymbol.getFriendlyName());
  }

  @Test
  void testParameterisedFunctionSymbolScope() {
    var t = support.createGenericT("Tee", symbolTable);
    var fun = support.createTemplateGenericFunction("fun", symbolTable, t);
    symbolTable.define(fun);

    //We've not defined the return type of the function
    assertEquals("public Unknown <- fun() of type Tee", fun.getFriendlyName());

    fun.setReturningSymbol(
        new VariableSymbol("rtn", symbolTable.resolve(new TypeSymbolSearch("Integer"))));
    assertEquals("public Integer <- fun() of type Tee", fun.getFriendlyName());

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
        new ScopedSymbol(IScope.ScopeType.AGGREGATE, "aggregateScope", symbolTable)));
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

    scopedSymbol.define(
        new VariableSymbol("check", scopedSymbol.resolve(new TypeSymbolSearch("String"))));
    assertTrue(scopedSymbol.resolve(new SymbolSearch("check")).isPresent());

    var clonedSymbol = scopedSymbol.clone(symbolTable);
    assertTrue(clonedSymbol.resolve(new SymbolSearch("check")).isPresent());

    return clonedSymbol;
  }
}

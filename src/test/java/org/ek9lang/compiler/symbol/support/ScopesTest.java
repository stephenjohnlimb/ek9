package org.ek9lang.compiler.symbol.support;

import org.ek9lang.compiler.symbol.*;
import org.ek9lang.compiler.symbol.support.search.SymbolSearch;
import org.ek9lang.compiler.symbol.support.search.TypeSymbolSearch;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Aimed at testing scopes and in some cases scoped symbols.
 *
 * Does not fully test aggregates and the like, just their scoped type natures.
 */
public class ScopesTest extends AbstractSymbolTestBase
{
	@Test
	public void testLocalScope()
	{
		var local = new LocalScope(symbolTable);

		assertNotNull(local);
		assertEquals(IScope.ScopeType.BLOCK, local.getScopeType());
		assertFalse(local.isMarkedPure());
		assertTrue(local.isScopeAMatchForEnclosingScope(symbolTable));
		assertFalse(local.isScopeAMatchForEnclosingScope(new SymbolTable()));
	}

	@Test
	public void testNamedScope()
	{
		//It's not really an aggregate scope but let;s test that type of setting.
		VariableSymbol v1 = new VariableSymbol("v3", symbolTable.resolve(new TypeSymbolSearch("Integer")));

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
	public void testFindingAggregateScope()
	{
		var aggregateScope1 = new ScopedSymbol(IScope.ScopeType.AGGREGATE, "aggregateScope", symbolTable);
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
	public void testAggregateSymbolScope()
	{
		//So this would be an actual 'type' like an OOP 'Customer' for example
		assertNotNull(checkScopedSymbol(new AggregateSymbol("aggregate", symbolTable)));
	}

	@Test
	public void testAggregateWithTraitsSymbolScope()
	{
		//This would be an aggregate that implements a number of traits.
		assertNotNull(checkScopedSymbol(new AggregateWithTraitsSymbol("aggregateWithTraits", symbolTable)));
	}

	@Test
	public void testParameterisedTypeSymbolScope()
	{
		var t = support.createGenericT("Tee", symbolTable);
		var z = support.createTemplateGenericType("Zee", symbolTable, t);
		symbolTable.define(z);

		assertEquals("Zee of type Tee", z.getFriendlyName());
		//This would be a concrete Zee with a concrete type of String to replace 'Tee'
		var pTypeSymbol = new ParameterisedTypeSymbol(z, symbolTable.resolve(new TypeSymbolSearch("String")), symbolTable);
		assertNotNull(checkScopedSymbol(pTypeSymbol));

		assertEquals("Zee of String", pTypeSymbol.getFriendlyName());
	}

	@Test
	public void testParameterisedFunctionSymbolScope()
	{
		var t = support.createGenericT("Tee", symbolTable);
		var fun = support.createTemplateGenericFunction("fun", symbolTable, t);
		symbolTable.define(fun);

		//We've not defined the return type of the function
		assertEquals("public Unknown <- fun() of type Tee", fun.getFriendlyName());

		fun.setReturningSymbol(new VariableSymbol("rtn", symbolTable.resolve(new TypeSymbolSearch("Integer"))));
		assertEquals("public Integer <- fun() of type Tee", fun.getFriendlyName());

		//This would be a concrete 'fun' with a concrete type of String to replace 'Tee'
		var pFun = new ParameterisedFunctionSymbol(fun, symbolTable.resolve(new TypeSymbolSearch("String")), symbolTable);

		assertNotNull(pFun.getReturningSymbol());
		var clonedPFun = checkScopedSymbol(pFun);
		assertNotNull(clonedPFun);

		//The return is the function should be the return type.
		System.out.println(pFun.getFriendlyName());

		System.out.println(clonedPFun.getFriendlyName());
	}

	@Test
	public void testControlSymbolScope()
	{
		assertNotNull(checkScopedSymbol(new ControlSymbol("Control", symbolTable)));
	}

	@Test
	public void testScopedSymbolScope()
	{
		assertNotNull(checkScopedSymbol(new ScopedSymbol(IScope.ScopeType.AGGREGATE, "aggregateScope", symbolTable)));
	}

	@Test
	public void testForSymbolScope()
	{
		assertNotNull(checkScopedSymbol(new ForSymbol(symbolTable)));
	}

	@Test
	public void testMethodSymbolScope()
	{
		assertNotNull(checkScopedSymbol(new MethodSymbol("aMethod", symbolTable)));
	}

	@Test
	public void testFunctionSymbolScope()
	{
		assertNotNull(checkScopedSymbol(new FunctionSymbol("aFunction", symbolTable)));
	}

	@Test
	public void testCallSymbolScope()
	{
		assertNotNull(checkScopedSymbol(new CallSymbol("aMethodCall", symbolTable)));
	}

	@Test
	public void testServiceOperationSymbolScope()
	{
		assertNotNull(checkScopedSymbol(new ServiceOperationSymbol("aServiceOperationCall", symbolTable)));
	}

	@Test
	public void testStreamCallSymbolScope()
	{
		assertNotNull(checkScopedSymbol(new StreamCallSymbol("aStreamCall", symbolTable)));
	}

	@Test
	public void testSwitchSymbolScope()
	{
		assertNotNull(checkScopedSymbol(new SwitchSymbol(symbolTable)));
	}

	@Test
	public void testTrySymbolScope()
	{
		assertNotNull(checkScopedSymbol(new TrySymbol(symbolTable)));
	}

	private ScopedSymbol checkScopedSymbol(ScopedSymbol scopedSymbol)
	{
		assertNotNull(scopedSymbol);

		scopedSymbol.define(new VariableSymbol("check", scopedSymbol.resolve(new TypeSymbolSearch("String"))));
		assertTrue(scopedSymbol.resolve(new SymbolSearch("check")).isPresent());

		var clonedSymbol = scopedSymbol.clone(symbolTable);
		assertTrue(clonedSymbol.resolve(new SymbolSearch("check")).isPresent());

		return clonedSymbol;
	}
}

package org.ek9lang.compiler.symbol.support;

import junit.framework.TestCase;
import org.ek9lang.compiler.symbol.*;
import org.ek9lang.compiler.symbol.support.search.SymbolSearch;
import org.ek9lang.compiler.symbol.support.search.TypeSymbolSearch;
import org.junit.Test;

import java.util.Optional;

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

		TestCase.assertNotNull(local);
		TestCase.assertEquals(IScope.ScopeType.BLOCK, local.getScopeType());
		TestCase.assertFalse(local.isMarkedPure());
		TestCase.assertTrue(local.isScopeAMatchForEnclosingScope(symbolTable));
		TestCase.assertFalse(local.isScopeAMatchForEnclosingScope(new SymbolTable()));
	}

	@Test
	public void testNamedScope()
	{
		//It's not really an aggregate scope but let;s test that type of setting.
		var local = new LocalScope(IScope.ScopeType.AGGREGATE, "someName", symbolTable);
		TestCase.assertTrue(local.getScopeName().equals("someName"));
		TestCase.assertNotNull(local);
		TestCase.assertEquals(IScope.ScopeType.AGGREGATE, local.getScopeType());

		TestCase.assertEquals("someName", local.getFriendlyScopeName());
	}

	@Test
	public void testFindingAggregateScope()
	{
		var aggregateScope1 = new ScopedSymbol(IScope.ScopeType.AGGREGATE, "aggregateScope", symbolTable);
		TestCase.assertFalse(aggregateScope1.isMarkedPure());

		var blockScope1 = new ScopedSymbol(IScope.ScopeType.BLOCK, "blockScope", aggregateScope1);

		var local = new LocalScope("JustLocalBlock", blockScope1);

		var foundScope = local.findNearestAggregateScopeInEnclosingScopes();
		TestCase.assertTrue(foundScope.isPresent());
		TestCase.assertTrue(aggregateScope1.equals(foundScope.get()));

		TestCase.assertTrue(blockScope1.isScopeAMatchForEnclosingScope(aggregateScope1));
	}

	@Test
	public void testAggregateSymbolScope()
	{
		//So this would be an actual 'type' like an OOP 'Customer' for example
		TestCase.assertNotNull(checkScopedSymbol(new AggregateSymbol("aggregate", symbolTable)));
	}

	@Test
	public void testAggregateWithTraitsSymbolScope()
	{
		//This would be an aggregate that implements a number of traits.
		TestCase.assertNotNull(checkScopedSymbol(new AggregateWithTraitsSymbol("aggregateWithTraits", symbolTable)));
	}

	@Test
	public void testParameterisedTypeSymbolScope()
	{
		var t = support.createGenericT("Tee", symbolTable);
		var z = support.createTemplateGenericType("Zee", symbolTable, t);
		symbolTable.define(z);

		TestCase.assertEquals("Zee of type Tee", z.getFriendlyName());
		//This would be a concrete Zee with a concrete type of String to replace 'Tee'
		var pTypeSymbol = new ParameterisedTypeSymbol(z, symbolTable.resolve(new TypeSymbolSearch("String")), symbolTable);
		TestCase.assertNotNull(checkScopedSymbol(pTypeSymbol));

		TestCase.assertEquals("Zee of String", pTypeSymbol.getFriendlyName());
	}

	@Test
	public void testParameterisedFunctionSymbolScope()
	{
		var t = support.createGenericT("Tee", symbolTable);
		var fun = support.createTemplateGenericFunction("fun", symbolTable, t);
		symbolTable.define(fun);

		//We've not defined the return type of the function
		TestCase.assertEquals("public Unknown <- fun() of type Tee", fun.getFriendlyName());

		fun.setReturningSymbol(new VariableSymbol("rtn", symbolTable.resolve(new TypeSymbolSearch("Integer"))));
		TestCase.assertEquals("public Integer <- fun() of type Tee", fun.getFriendlyName());

		//This would be a concrete 'fun' with a concrete type of String to replace 'Tee'
		var pFun = new ParameterisedFunctionSymbol(fun, symbolTable.resolve(new TypeSymbolSearch("String")), symbolTable);

		TestCase.assertNotNull(pFun.getReturningSymbol());
		var clonedPFun = checkScopedSymbol(pFun);
		TestCase.assertNotNull(clonedPFun);

		//The return is the function should be the return type.
		System.out.println(pFun.getFriendlyName());

		System.out.println(clonedPFun.getFriendlyName());
	}

	@Test
	public void testControlSymbolScope()
	{
		TestCase.assertNotNull(checkScopedSymbol(new ControlSymbol("Control", symbolTable)));
	}

	@Test
	public void testScopedSymbolScope()
	{
		TestCase.assertNotNull(checkScopedSymbol(new ScopedSymbol(IScope.ScopeType.AGGREGATE, "aggregateScope", symbolTable)));
	}

	@Test
	public void testForSymbolScope()
	{
		TestCase.assertNotNull(checkScopedSymbol(new ForSymbol(symbolTable)));
	}

	@Test
	public void testMethodSymbolScope()
	{
		TestCase.assertNotNull(checkScopedSymbol(new MethodSymbol("aMethod", symbolTable)));
	}

	@Test
	public void testFunctionSymbolScope()
	{
		TestCase.assertNotNull(checkScopedSymbol(new FunctionSymbol("aFunction", symbolTable)));
	}

	@Test
	public void testCallSymbolScope()
	{
		TestCase.assertNotNull(checkScopedSymbol(new CallSymbol("aMethodCall", symbolTable)));
	}

	@Test
	public void testServiceOperationSymbolScope()
	{
		TestCase.assertNotNull(checkScopedSymbol(new ServiceOperationSymbol("aServiceOperationCall", symbolTable)));
	}

	@Test
	public void testStreamCallSymbolScope()
	{
		TestCase.assertNotNull(checkScopedSymbol(new StreamCallSymbol("aStreamCall", symbolTable)));
	}

	@Test
	public void testSwitchSymbolScope()
	{
		TestCase.assertNotNull(checkScopedSymbol(new SwitchSymbol(symbolTable)));
	}

	@Test
	public void testTrySymbolScope()
	{
		TestCase.assertNotNull(checkScopedSymbol(new TrySymbol(symbolTable)));
	}

	private ScopedSymbol checkScopedSymbol(ScopedSymbol scopedSymbol)
	{
		TestCase.assertNotNull(scopedSymbol);

		scopedSymbol.define(new VariableSymbol("check", scopedSymbol.resolve(new TypeSymbolSearch("String"))));
		TestCase.assertTrue(scopedSymbol.resolve(new SymbolSearch("check")).isPresent());

		var clonedSymbol = scopedSymbol.clone(symbolTable);
		TestCase.assertTrue(clonedSymbol.resolve(new SymbolSearch("check")).isPresent());

		return clonedSymbol;
	}
}

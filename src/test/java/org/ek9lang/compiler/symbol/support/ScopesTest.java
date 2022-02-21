package org.ek9lang.compiler.symbol.support;

import junit.framework.TestCase;
import org.ek9lang.compiler.symbol.IScope;
import org.ek9lang.compiler.symbol.LocalScope;
import org.ek9lang.compiler.symbol.ScopedSymbol;
import org.junit.Test;

/**
 * Aimed at testing scopes and in some cases scoped symbols.
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
}

package org.ek9lang.compiler.symbol.support;


import junit.framework.TestCase;
import org.ek9lang.compiler.symbol.LocalScope;
import org.junit.Test;

/**
 * Just simple tests of the scope stack that will be used in the
 * compiler phases when building the internal model and symbols.
 */
public class ScopeStackTest
{
	@Test
	public void testNewScopeStack()
	{
		ScopeStack underTest = new ScopeStack(new SymbolTable());
		TestCase.assertFalse(underTest.empty());

		var theTop = underTest.getVeryBaseScope();
		TestCase.assertNotNull(theTop);
		var top = underTest.pop();

		TestCase.assertTrue(top == theTop);
		TestCase.assertNotNull(top);
		TestCase.assertTrue(underTest.empty());
	}

	@Test
	public void testPushOnScopeStack()
	{
		ScopeStack underTest = new ScopeStack(new SymbolTable());
		TestCase.assertFalse(underTest.empty());

		underTest.push(new LocalScope("A test", new SymbolTable()));

		var scope = underTest.pop();
		TestCase.assertNotNull(scope);
		TestCase.assertFalse(underTest.empty());

		var top = underTest.pop();
		TestCase.assertNotNull(top);

		TestCase.assertTrue(underTest.empty());
	}

}

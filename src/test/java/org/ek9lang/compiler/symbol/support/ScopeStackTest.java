package org.ek9lang.compiler.symbol.support;


import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.ek9lang.compiler.symbol.LocalScope;

/**
 * Just simple tests of the scope stack that will be used in the
 * compiler phases when building the internal model and symbols.
 */
class ScopeStackTest
{
	@Test
	void testNewScopeStack()
	{
		ScopeStack underTest = new ScopeStack(new SymbolTable());
		assertFalse(underTest.empty());

		var theTop = underTest.getVeryBaseScope();
		assertNotNull(theTop);
		var top = underTest.pop();

		assertSame(top, theTop);
		assertNotNull(top);
		assertTrue(underTest.empty());
	}

	@Test
	void testPushOnScopeStack()
	{
		ScopeStack underTest = new ScopeStack(new SymbolTable());
		assertFalse(underTest.empty());

		underTest.push(new LocalScope("A test", new SymbolTable()));

		var scope = underTest.pop();
		assertNotNull(scope);
		assertFalse(underTest.empty());

		var top = underTest.pop();
		assertNotNull(top);

		assertTrue(underTest.empty());
	}

}

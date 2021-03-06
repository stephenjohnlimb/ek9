package org.ek9lang.compiler.errors;

import org.antlr.v4.runtime.Token;
import org.ek9lang.compiler.symbol.AggregateSymbol;
import org.ek9lang.compiler.symbol.VariableSymbol;
import org.ek9lang.compiler.symbol.support.SymbolTable;
import org.ek9lang.compiler.symbol.support.search.MatchResult;
import org.ek9lang.compiler.symbol.support.search.MatchResults;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Most of the use of the ErrorListener will be driven by examples of
 * EK9 source code that is designed to 'fail' to compile.
 * <p>
 * These examples of EK9 that fail to compile should be stored in 'badExamples' in the
 * resources' directory. Ideally these bad example will be grouped in to subdirectories.
 * <p>
 * So this unit test is just designed to test the basic mechanisms.
 */
public final class ErrorListenerTest
{
	@Test
	public void testReturningRequired()
	{
		ErrorListener underTest = new ErrorListener();
		underTest.raiseReturningRequired(createSyntheticToken(), "_EK9 Test");
		assertInError(underTest);
	}

	@Test
	public void testReturningRedundant()
	{
		ErrorListener underTest = new ErrorListener();
		underTest.raiseReturningRedundant(createSyntheticToken(), "_EK9 Test");
		assertInError(underTest);
	}

	@Test
	public void testSemanticErrorCreationWithModule()
	{
		ErrorListener underTest = new ErrorListener();
		underTest.semanticError(createSyntheticToken(), "_EK9 Test", ErrorListener.SemanticClassification.METHOD_AMBIGUOUS);
		assertInError(underTest);
	}

	@Test
	public void testSemanticErrorCreation()
	{
		ErrorListener underTest = new ErrorListener();
		underTest.semanticError(createSyntheticToken(), "_EK9 Test", ErrorListener.SemanticClassification.METHOD_AMBIGUOUS);
		assertInError(underTest);
	}

	@Test
	public void testSemanticErrorCreationNoToken()
	{
		ErrorListener underTest = new ErrorListener();
		underTest.semanticError(null, "Test", ErrorListener.SemanticClassification.METHOD_AMBIGUOUS);
		assertInError(underTest);
	}

	@Test
	public void testSemanticErrorFuzzyResults()
	{
		var symbolTable = new SymbolTable();
		AggregateSymbol integerType = new AggregateSymbol("Integer", symbolTable);
		symbolTable.define(integerType);

		ErrorListener underTest = new ErrorListener();
		MatchResults results = new MatchResults(5);

		//Just do some simple checks on the results match code
		results.add(new MatchResult(2, new VariableSymbol("v2", integerType)));
		assertEquals(1, results.size());
		results.add(new MatchResult(5, new VariableSymbol("v5", integerType)));
		assertEquals(2, results.size());

		results.add(new MatchResult(0, new VariableSymbol("top", integerType)));
		results.add(new MatchResult(1000, new VariableSymbol("worst", integerType)));

		results.add(new MatchResult(3, new VariableSymbol("v3", integerType)));
		assertEquals(5, results.size());
		assertEquals("top as Integer, v2 as Integer, v3 as Integer, v5 as Integer, worst as Integer", results.toString());

		//Now lets check the priority functionality by adding more
		results.add(new MatchResult(1, new VariableSymbol("v1", integerType)));
		results.add(new MatchResult(4, new VariableSymbol("v4", integerType)));
		assertEquals(5, results.size());
		assertEquals("top as Integer, v1 as Integer, v2 as Integer, v3 as Integer, v4 as Integer", results.toString());

		var iter = results.iterator();
		while(iter.hasNext())
			assertNotNull(iter.next().getSymbol());

		underTest.semanticError(createSyntheticToken(), "Fuzzy", ErrorListener.SemanticClassification.CONSTRUCTOR_NOT_RESOLVED, results);
		assertInError(underTest);
	}

	private void assertInError(ErrorListener underTest)
	{
		assertFalse(underTest.isErrorFree());
		ErrorListener.ErrorDetails details = underTest.getErrors().next();
		assertNotNull(details);
		assertNotNull(details.getTypeOfError());
		assertEquals(0, details.getPosition());
		assertEquals(0, details.getLineNumber());
		assertNotNull(details.getLikelyOffendingSymbol());
		assertNotNull(details.toString());
	}

	@Test
	public void testConstructionAndSetup()
	{
		ErrorListener underTest = new ErrorListener();

		//Setup check defaults and ensure switches work.
		assertTrue(underTest.isErrorFree());
		assertTrue(underTest.isWarningFree());

		//These are the three main flags that indicate there might be an issue with the grammar.
		assertFalse(underTest.isExceptionOnAmbiguity());
		underTest.setExceptionOnAmbiguity(true);
		assertTrue(underTest.isExceptionOnAmbiguity());

		assertFalse(underTest.isExceptionOnContextSensitive());
		underTest.setExceptionOnContextSensitive(true);
		assertTrue(underTest.isExceptionOnContextSensitive());

		assertFalse(underTest.isExceptionOnFullContext());
		underTest.setExceptionOnFullContext(true);
		assertTrue(underTest.isExceptionOnFullContext());

		Token token = createSyntheticToken();
		underTest.raiseReturningRedundant(token, "Test Message");

		//Should now be in error
		assertFalse(underTest.isErrorFree());
		ErrorListener.ErrorDetails details = underTest.getErrors().next();
		assertNotNull(details);
		assertNotNull(details.getClassification());

		assertNotNull(details.toString());
		assertEquals(ErrorListener.SemanticClassification.RETURNING_REDUNDANT, details.getSemanticClassification());

		assertTrue(underTest.isWarningFree());

		//Now create a warning
		underTest.semanticWarning(createSyntheticToken(), "Test Warning Message", ErrorListener.SemanticClassification.METHOD_AMBIGUOUS);
		assertFalse(underTest.isWarningFree());

		details = underTest.getWarnings().next();
		assertNotNull(details.toString());
		assertEquals(ErrorListener.SemanticClassification.METHOD_AMBIGUOUS, details.getSemanticClassification());

	}

	private Token createSyntheticToken()
	{
		return new org.ek9lang.compiler.tokenizer.SyntheticToken();
	}
}

package org.ek9lang.compiler.symbol.support;

import org.ek9lang.compiler.symbol.*;
import org.ek9lang.compiler.symbol.support.search.*;
import org.ek9lang.compiler.tokenizer.SyntheticToken;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test out the basic operations on the symbol table.
 * The Symbol Table is used in many contexts.
 */
public class SymbolTableTest
{

	@Test
	public void testSearchEmptySymbolTable()
	{
		SymbolTable underTest = new SymbolTable();
		assertNotFound(underTest);
	}

	@Test
	public void testUnresolvedSymbolTable()
	{
		//Define some symbols - but not the ones we're going to look up.
		SymbolTable underTest = new SymbolTable();
		underTest.define(new ConstantSymbol("notThisConstant"));
		underTest.define(new MethodSymbol("notThisMethod", underTest));

		underTest.define(new FunctionSymbol("notThisFunction", underTest));
		underTest.define(new AggregateSymbol("notThisType", underTest));

		//This turns the functions/aggregates into Template Generic types
		underTest.define(new FunctionSymbol("notThisFunction", underTest).addParameterisedType(new AggregateSymbol("NoneSuch1", underTest)));
		underTest.define(new AggregateSymbol("notThisType", underTest).addParameterisedType(new AggregateSymbol("NoneSuch2", underTest)));

		assertNotFound(underTest);
	}

	private void assertNotFound(SymbolTable underTest)
	{
		assertFalse(underTest.resolve(new AnySymbolSearch("SomeThing")).isPresent());
		assertFalse(underTest.resolve(new AnySymbolSearch("Fully.Qualified::SomeThing")).isPresent());

		//By specific search types.
		assertFalse(underTest.resolve(new TypeSymbolSearch("SomeThing")).isPresent());
		assertFalse(underTest.resolve(new TypeSymbolSearch("Fully.Qualified::SomeThing")).isPresent());

		assertFalse(underTest.resolve(new TemplateTypeSymbolSearch("SomeThing")).isPresent());
		assertFalse(underTest.resolve(new TemplateTypeSymbolSearch("Fully.Qualified::SomeThing")).isPresent());

		assertFalse(underTest.resolve(new FunctionSymbolSearch("SomeThing")).isPresent());
		assertFalse(underTest.resolve(new FunctionSymbolSearch("Fully.Qualified::SomeThing")).isPresent());

		assertFalse(underTest.resolve(new TemplateFunctionSymbolSearch("SomeThing")).isPresent());
		assertFalse(underTest.resolve(new TemplateFunctionSymbolSearch("Fully.Qualified::SomeThing")).isPresent());

		assertFalse(underTest.resolveInThisScopeOnly(new TemplateFunctionSymbolSearch("SomeThing")).isPresent());
		assertFalse(underTest.resolve(new MethodSymbolSearch("SomeThing")).isPresent());
		MethodSymbolSearchResult result = new MethodSymbolSearchResult();
		assertTrue(underTest.resolveForAllMatchingMethods(new MethodSymbolSearch("SomeThing"), result).isEmpty());
	}

	@Test
	public void testEncounteredExceptionToken()
	{
		SymbolTable underTest = new SymbolTable();
		assertNull(underTest.getEncounteredExceptionToken());

		underTest.setEncounteredExceptionToken(new SyntheticToken());
		assertNotNull(underTest.getEncounteredExceptionToken());
	}

	@Test
	public void testMethodDefinition()
	{
		SymbolTable underTest = new SymbolTable();

		//A simple method (I know it's just in a symbol table), don't define any parameters or returns.
		var methodName = "method1";
		underTest.define(new MethodSymbol(methodName, underTest));
		assertMethodPresentInSymbolTable(underTest, methodName);
	}

	@Test
	public void testCloneSymbolTable()
	{
		SymbolTable underTest = new SymbolTable();

		//A simple method (I know it's just in a symbol table), don't define any parameters or returns.
		var methodName = "method1";
		underTest.define(new MethodSymbol(methodName, underTest));
		assertMethodPresentInSymbolTable(underTest, methodName);

		//Now clone but with an empty enclosing scope, we should still be able to find the same method
		assertMethodPresentInSymbolTable(underTest.clone(new SymbolTable()), methodName);
	}

	private void assertMethodPresentInSymbolTable(SymbolTable underTest, String methodName)
	{
		var anySymbolSearch = new AnySymbolSearch(methodName);
		Optional<ISymbol> searchResult = underTest.resolve(anySymbolSearch);
		assertTrue(searchResult.isPresent());
		assertTrue(methodName.equals(searchResult.get().getName()));

		//Now clone search and check again!
		underTest.resolve(anySymbolSearch.clone());
		assertTrue(searchResult.isPresent());
		assertTrue(methodName.equals(searchResult.get().getName()));

		MethodSymbolSearch methodSearch = new MethodSymbolSearch(methodName);
		assertTrue(methodSearch.toString() != null);
		assertTrue(methodSearch.getNameAsSymbol().isPresent());

		searchResult = underTest.resolve(methodSearch);
		assertTrue(searchResult.isPresent());
		assertTrue(methodName.equals(searchResult.get().getName()));

		// Now clone that search and check again
		searchResult = underTest.resolve(methodSearch.clone());
		assertTrue(searchResult.isPresent());
		assertTrue(methodName.equals(searchResult.get().getName()));

		//Should not find as a type.
		searchResult = underTest.resolve(new TypeSymbolSearch(methodName));
		assertFalse(searchResult.isPresent());

		MethodSymbolSearchResult result = new MethodSymbolSearchResult();
		result = underTest.resolveForAllMatchingMethods(new MethodSymbolSearch(methodName), result);
		assertFalse(result.isEmpty());
		assertFalse(result.isAmbiguous());
		assertNotNull(result.toString());
		assertTrue(result.getSingleBestMatchSymbol().isPresent());
	}

	@Test
	public void testVariableDefinitionInMethod()
	{
		SymbolTable globalSymbolTable = new SymbolTable();
		MethodSymbol method1 = new MethodSymbol("method1", globalSymbolTable);
		VariableSymbol variableSymbol = new VariableSymbol("var1", method1);

		method1.define(variableSymbol);

		globalSymbolTable.define(method1);

		//Ok so now have a variable defined in a method in a global symbol table.
		//Find the variable.
		Optional<ISymbol> searchResult = method1.resolve(new AnySymbolSearch("var1").setLimitToBlocks(true));
	}

	@Test
	public void testMethodWithParameters()
	{
		SymbolTable globalSymbolTable = new SymbolTable();
		assertEquals(IScope.ScopeType.BLOCK, globalSymbolTable.getScopeType());

		ISymbol floatType = new AggregateSymbol("Float", globalSymbolTable);
		ISymbol integerType = new AggregateSymbol("Integer", globalSymbolTable);
		ISymbol stringType = new AggregateSymbol("String", globalSymbolTable);

		MethodSymbol method1 = new MethodSymbol("method1", globalSymbolTable);
		VariableSymbol returningSymbol = new VariableSymbol("rtn", floatType);
		method1.setReturningSymbol(returningSymbol);
		globalSymbolTable.define(method1);

		//OK but now lets add parameters to it.
		method1.define(new VariableSymbol("p1", floatType));
		method1.define(new VariableSymbol("p2", integerType));
		method1.define(new VariableSymbol("p3", stringType));

		//So it could now be called as method1(56.9, 22, "steve")
		//Now find it and also fail to find it.
		MethodSymbolSearch symbolSearch = new MethodSymbolSearch(method1);
		assertEquals(3, symbolSearch.getParameterTypes().size());
		assertTrue("method1(p1 as Float, p2 as Integer, p3 as String)".equals(symbolSearch.toString()));
		Optional<ISymbol> resolvedMethod = globalSymbolTable.resolve(symbolSearch);
		assertTrue(resolvedMethod.isPresent());

		//Now lets try a more details clone of the method symbol search
		resolvedMethod = globalSymbolTable.resolve(symbolSearch.clone());
		assertTrue(resolvedMethod.isPresent());

		symbolSearch = new MethodSymbolSearch("method1");
		resolvedMethod = globalSymbolTable.resolve(symbolSearch);
		//method1 does exist but the search will not find it because no params added.
		assertFalse(resolvedMethod.isPresent());
	}

	@Test
	public void testMethodOverLoadedDefinition()
	{
		SymbolTable underTest = new SymbolTable();

		ISymbol floatType = new AggregateSymbol("Float", underTest);
		ISymbol integerType = new AggregateSymbol("Integer", underTest);
		ISymbol stringType = new AggregateSymbol("String", underTest);

		//create a method that returns a named variable that is a float
		MethodSymbol method1 = new MethodSymbol("method1", underTest);
		VariableSymbol returningSymbol = new VariableSymbol("rtn", floatType);
		method1.setReturningSymbol(returningSymbol);
		underTest.define(method1);

		Optional<ISymbol> searchResult = underTest.resolve(new AnySymbolSearch("method1"));
		assertTrue(searchResult.isPresent());
		assertTrue("method1".equals(searchResult.get().getName()));

		//Search by using a know method as a prototype for a search.
		searchResult = underTest.resolve(new MethodSymbolSearch(method1));
		assertTrue(searchResult.isPresent());
		assertTrue("method1".equals(searchResult.get().getName()));

		//Now search but looking for specific return Type.
		searchResult = underTest.resolve(new MethodSymbolSearch("method1", floatType));
		assertTrue(searchResult.isPresent());
		assertTrue("method1".equals(searchResult.get().getName()));

		//Look for same method but returning String, should fail
		searchResult = underTest.resolve(new MethodSymbolSearch("method1", Optional.of(stringType)));
		assertFalse(searchResult.isPresent());

		//Same again with Integer
		searchResult = underTest.resolve(new MethodSymbolSearch("method1").setOfTypeOrReturn(integerType));
		assertFalse(searchResult.isPresent());

		//Now define again, should allow as overloading
		underTest.define(new MethodSymbol("method1", underTest));
		searchResult = underTest.resolve(new MethodSymbolSearch("method1"));
		//But now it has become ambiguous and so will fail to resolve.
		assertFalse(searchResult.isPresent());
	}

	@Test
	public void testFunctionDefinition()
	{
		SymbolTable underTest = new SymbolTable();

		//A simple function, don't define any parameters or returns.
		underTest.define(new FunctionSymbol("function1", underTest));
		Optional<ISymbol> searchResult = underTest.resolve(new AnySymbolSearch("function1"));
		assertTrue(searchResult.isPresent());
		assertTrue("function1".equals(searchResult.get().getName()));

		var functionSymbolSearch = new FunctionSymbolSearch("function1");
		searchResult = underTest.resolve(functionSymbolSearch);
		assertTrue(searchResult.isPresent());
		assertTrue("function1".equals(searchResult.get().getName()));

		//Clone search and try again.
		searchResult = underTest.resolve(functionSymbolSearch.clone());
		assertTrue(searchResult.isPresent());
		assertTrue("function1".equals(searchResult.get().getName()));

		//This turns the functions/aggregates into Template Generic types
		underTest.define(new FunctionSymbol("function2", underTest).addParameterisedType(new AggregateSymbol("NoneSuch1", underTest)));

		var templateFunctionSymbolSearch = new TemplateFunctionSymbolSearch("function2");
		searchResult = underTest.resolve(templateFunctionSymbolSearch);
		assertTrue(searchResult.isPresent());
		assertTrue("function2".equals(searchResult.get().getName()));

		//Clone search and try again
		searchResult = underTest.resolve(templateFunctionSymbolSearch.clone());
		assertTrue(searchResult.isPresent());
		assertTrue("function2".equals(searchResult.get().getName()));

		//Should not find as a type.
		searchResult = underTest.resolve(new TypeSymbolSearch("function1"));
		assertFalse(searchResult.isPresent());

		searchResult = underTest.resolve(new TypeSymbolSearch("function2"));
		assertFalse(searchResult.isPresent());
	}

	@Test
	public void testDuplicateFunctionDefinition()
	{
		assertThrows(java.lang.RuntimeException.class, () -> {
			SymbolTable underTest = new SymbolTable();

			//duplicate check
			underTest.define(new FunctionSymbol("function1", underTest));
			underTest.define(new FunctionSymbol("function1", underTest));
		});
	}

	@Test
	public void testConstantDefinition()
	{
		SymbolTable underTest = new SymbolTable();
		//We should be able to add it without saying what type it is
		underTest.define(new ConstantSymbol("PI"));
		Optional<ISymbol> searchResult = underTest.resolve(new AnySymbolSearch("PI"));
		assertTrue(searchResult.isPresent());
		assertTrue("PI".equals(searchResult.get().getName()));
	}

	@Test
	public void testConstantDefinitionInvalidType()
	{
		SymbolTable underTest = new SymbolTable();
		//We should be able to add it without saying what type it is
		underTest.define(new ConstantSymbol("PI"));
		//But if we search and ask for a specific type of return then it should not be found.
		SymbolSearch search = new AnySymbolSearch("PI").setOfTypeOrReturn(new AggregateSymbol("Float", underTest));
		Optional<ISymbol> searchResult = underTest.resolve(search);
		assertFalse(searchResult.isPresent());
	}

	@Test
	public void testConstantDefinitionValidType()
	{
		SymbolTable underTest = new SymbolTable();
		//This time give it a type.
		underTest.define(new ConstantSymbol("PI").setType(new AggregateSymbol("Float", underTest)));
		//But if we search and ask for a specific type of return then it should not be found.
		SymbolSearch search = new AnySymbolSearch("PI").setOfTypeOrReturn(new AggregateSymbol("Float", underTest));
		Optional<ISymbol> searchResult = underTest.resolve(search);
		//Now it should be found
		assertTrue(searchResult.isPresent());
		assertTrue("PI".equals(searchResult.get().getName()));

		//It should also be found if we don't request a specific return type.
		searchResult = underTest.resolve(new AnySymbolSearch("PI"));
		//Now it should be found
		assertTrue(searchResult.isPresent());
		assertTrue("PI".equals(searchResult.get().getName()));
	}

	@Test
	public void testDuplicateConstantDefinition()
	{
		assertThrows(java.lang.RuntimeException.class, () -> {
			SymbolTable underTest = new SymbolTable();
			underTest.define(new ConstantSymbol("PI"));
			//Check duplications detected.
			underTest.define(new ConstantSymbol("PI"));
		});
	}

	@Test
	public void testBasicTypeDefinitionAndLookup()
	{
		SymbolTable underTest = new SymbolTable();
		assertTrue(underTest.getScopeName().equals("global"));
		assertTrue("global".equals(underTest.toString()));

		assertFalse(underTest.resolve(new TypeSymbolSearch("Float")).isPresent());

		AggregateSymbol floatType = new AggregateSymbol("Float", underTest);
		floatType.setSourceToken(new SyntheticToken());
		assertNotNull(floatType.getSourceToken());
		underTest.define(floatType);


		List<ISymbol> symbols = underTest.getSymbolsForThisScope();
		assertEquals(1, symbols.size());
		assertTrue("Float".equals(symbols.get(0).getName()));

		symbols = underTest.getSymbolsForThisScopeOfCategory(ISymbol.SymbolCategory.TYPE);
		assertEquals(1, symbols.size());
		assertTrue("Float".equals(symbols.get(0).getName()));

		//Now do a type symbol search for a type we know must be there
		Optional<ISymbol> searchResult = underTest.resolve(new TypeSymbolSearch("Float"));
		assertTrue(searchResult.isPresent());
		assertTrue("Float".equals(searchResult.get().getName()));

		//Now search via fully qualified name
		searchResult = underTest.resolve(new TypeSymbolSearch("global::Float"));
		assertTrue(searchResult.isPresent());
		assertTrue("Float".equals(searchResult.get().getName()));

		//Now search via any search.
		searchResult = underTest.resolve(new AnySymbolSearch("Float"));
		assertTrue(searchResult.isPresent());
		assertTrue("Float".equals(searchResult.get().getName()));

		//Now do a type symbol search for a type we know can't be there
		var typeSymbolSearch = new TypeSymbolSearch("Integer");
		searchResult = underTest.resolveInThisScopeOnly(typeSymbolSearch);
		assertFalse(searchResult.isPresent());

		//Clone and try again
		searchResult = underTest.resolveInThisScopeOnly(typeSymbolSearch.clone());
		assertFalse(searchResult.isPresent());

		//Has no enclosing scope so expect false.
		assertFalse(underTest.findNearestAggregateScopeInEnclosingScopes().isPresent());
		assertFalse(underTest.isScopeAMatchForEnclosingScope(underTest));
		assertFalse(underTest.resolveWithEnclosingScope(new TypeSymbolSearch("Float")).isPresent());

		AggregateSymbol templateType = new AggregateSymbol("Special", underTest);
		templateType.addParameterisedType(new AggregateSymbol("T", underTest));
		underTest.define(templateType);

		var templateTypeSymbolSearch = new TemplateTypeSymbolSearch("Special");
		searchResult = underTest.resolveInThisScopeOnly(templateTypeSymbolSearch);
		assertTrue(searchResult.isPresent());

		//Clone search and check again
		searchResult = underTest.resolveInThisScopeOnly(templateTypeSymbolSearch.clone());
		assertTrue(searchResult.isPresent());

	}
}

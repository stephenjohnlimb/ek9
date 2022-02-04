package org.ek9lang.compiler.symbol.support;

import junit.framework.TestCase;
import org.ek9lang.compiler.symbol.*;
import org.ek9lang.compiler.symbol.support.search.MethodSymbolSearch;
import org.ek9lang.compiler.symbol.support.search.SymbolSearch;
import org.ek9lang.compiler.symbol.support.search.TypeSymbolSearch;
import org.ek9lang.compiler.tokenizer.SyntheticToken;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

public class SymbolsTest extends AbstractSymbolTestBase
{
	@Test
	public void testFullyQualifiedName()
	{
		TestCase.assertFalse(support.isSymbolNameFullyQualified("name"));
		TestCase.assertTrue(support.isSymbolNameFullyQualified("com.name::name"));

		TestCase.assertTrue("com.part".equals(support.getModuleNameIfPresent("com.part::name")));
		TestCase.assertTrue("".equals(support.getModuleNameIfPresent("name")));

		TestCase.assertTrue("name".equals(support.getUnqualifiedName("com.part::name")));
		TestCase.assertTrue("name".equals(support.getUnqualifiedName("name")));
	}

	@Test
	public void testCreateGenericTypeT()
	{
		AggregateSymbol t = support.createGenericT("T", symbolTable);
		TestCase.assertNotNull(t);
		TestCase.assertFalse(t.getSymbolsForThisScope().isEmpty());
	}

	@Test
	public void testTypeCloningMechanism()
	{
		var from = createBasicAggregate("From");
		//Now add in an additional method.
		Optional<ISymbol> integerType = symbolTable.resolve(new TypeSymbolSearch("Integer"));
		var newMethod1 = support.addPublicMethod(from, "someMethod", List.of(), Optional.of(from));

		var var1 = new VariableSymbol("v1", integerType);
		var newMethod2 = support.addPublicMethod(from, "someMethod", List.of(var1), Optional.of(from));

		var newMethod3 = support.addPublicMethod(from, "someAbstractMethod", List.of(var1), Optional.of(from));
		newMethod3.setMarkedAbstract(true);

		//Now lets clone those symbols over from "From" to "To" - but we only want the non-abstract methods.
		//But we should get the methods and also any public constructors if From had the public constructors.
		//This addNonAbstractMethods only applies to methods that return a type of "From" so not all abstract methods.
		var to = new AggregateSymbol("To", symbolTable);

		support.addNonAbstractMethods(from, to);

		var resolvedMethod1 = to.resolveInThisScopeOnly(new MethodSymbolSearch(newMethod1));
		TestCase.assertTrue(resolvedMethod1.isPresent());
		var resolvedMethod2 = to.resolveInThisScopeOnly(new MethodSymbolSearch(newMethod2));
		TestCase.assertTrue(resolvedMethod2.isPresent());

		var resolvedMethod3 = to.resolveInThisScopeOnly(new MethodSymbolSearch(newMethod3));
		TestCase.assertFalse(resolvedMethod3.isPresent());
	}

	@Test
	public void testCreateEnumerationType()
	{
		AggregateSymbol e = new AggregateSymbol("CheckEnum", symbolTable);
		support.addSyntheticEnumerationMethods(e);
		TestCase.assertFalse(e.getSymbolsForThisScope().isEmpty());
	}

	@Test
	public void testAggregateCreation()
	{
		Optional<ISymbol> stringType = symbolTable.resolve(new TypeSymbolSearch("String"));
		Optional<ISymbol> integerType = symbolTable.resolve(new TypeSymbolSearch("Integer"));

		AggregateSymbol underTest = createBasicAggregate("UnderTest");

		//Should have constructor
		Optional<ISymbol> resolvedMethod = underTest.resolveInThisScopeOnly(new MethodSymbolSearch("UnderTest"));
		TestCase.assertTrue(resolvedMethod.isPresent());

		//Add another constructor that takes a String as an argument
		SymbolSearch search1 = new MethodSymbolSearch("UnderTest")
				.addParameter(new VariableSymbol("arg1", stringType));

		resolvedMethod = underTest.resolveInThisScopeOnly(search1);
		TestCase.assertTrue(resolvedMethod.isPresent());

		//check constructor with String,Integer params does not exist then create it and check it does.
		SymbolSearch search2 = new MethodSymbolSearch("UnderTest")
				.addParameter(new VariableSymbol("arg1", stringType))
				.addParameter(new VariableSymbol("arg2", integerType));
		resolvedMethod = underTest.resolveInThisScopeOnly(search2);

		TestCase.assertFalse(resolvedMethod.isPresent());
		support.addSyntheticConstructorIfRequired(underTest);
		//Now that constructor should exist
		resolvedMethod = underTest.resolveInThisScopeOnly(search2);
		TestCase.assertTrue(resolvedMethod.isPresent());
	}

	private AggregateSymbol createBasicAggregate(String name)
	{
		AggregateSymbol rtn = new AggregateSymbol(name, symbolTable);
		//Add some fields/properties.
		Optional<ISymbol> stringType = symbolTable.resolve(new TypeSymbolSearch("String"));
		Optional<ISymbol> integerType = symbolTable.resolve(new TypeSymbolSearch("Integer"));
		TestCase.assertTrue(stringType.isPresent());
		TestCase.assertTrue(integerType.isPresent());

		//Add a couple of properties.
		rtn.define(new VariableSymbol("v1", stringType));
		rtn.define(new VariableSymbol("v2", integerType));

		support.addConstructor(rtn);
		//Add another constructor that takes a String as an argument
		MethodSymbol constructor2 = support.addConstructor(rtn, stringType);
		return rtn;
	}

	@Test
	public void testVariableSymbol()
	{
		IScope symbolTable = new SymbolTable();
		ISymbol integerType = new AggregateSymbol("Integer", symbolTable);

		//define without type first
		VariableSymbol v1 = new VariableSymbol("v1");
		assertVariable1(v1);
		assertVariable1(v1.clone(symbolTable));

		VariableSymbol v2 = new VariableSymbol("v2", Optional.of(integerType));
		v2.setInitialisedBy(new SyntheticToken());
		v2.setIncomingParameter(true);

		TestCase.assertTrue(v2.isInitialised());

		VariableSymbol v3 = new VariableSymbol("v3", Optional.of(integerType));
		v3.setReturningParameter(true);
		v3.setPrivate(true);
		TestCase.assertTrue(v3.isReturningParameter());
		TestCase.assertNotNull(v3.toString());

		VariableSymbol loopVar = new VariableSymbol("loopVar", Optional.of(integerType));
		loopVar.setLoopVariable(true);
		TestCase.assertTrue(loopVar.isLoopVariable());
		TestCase.assertTrue(loopVar.clone(symbolTable).isLoopVariable());

		TestCase.assertNotNull(loopVar.getFriendlyName());
		TestCase.assertNotNull(loopVar.getFullyQualifiedName());
		TestCase.assertNotNull(loopVar.getSourceFileLocation());

		TestCase.assertTrue(integerType.isAssignableTo(v3.getType()));
	}

	@Test
	public void testConstantSymbol()
	{
		IScope symbolTable = new SymbolTable();
		ISymbol integerType = new AggregateSymbol("Integer", symbolTable);
		ConstantSymbol c1 = new ConstantSymbol("1", true);
		c1.setType(integerType);
		c1.setAtModuleScope(true);
		c1.setNotMutable();

		assertConstant1(c1);
		assertConstant1(c1.clone(symbolTable));

		ConstantSymbol c2 = new ConstantSymbol("1", integerType, true);
		c2.setAtModuleScope(false);

		TestCase.assertTrue(c2.isAConstant());
		TestCase.assertTrue(c2.isMutable());
		TestCase.assertFalse(c2.isAtModuleScope());
		TestCase.assertTrue(c2.isFromLiteral());
		TestCase.assertTrue(c2.getGenus().equals(ISymbol.SymbolGenus.VALUE));

		ConstantSymbol c3 = new ConstantSymbol("1", integerType);
		TestCase.assertTrue(c3.getGenus().equals(ISymbol.SymbolGenus.VALUE));

	}

	private void assertConstant1(ConstantSymbol c)
	{
		TestCase.assertNotNull(c.getFriendlyName());
		TestCase.assertTrue(c.isAConstant());
		TestCase.assertFalse(c.isMutable());
		TestCase.assertTrue(c.isAtModuleScope());
		TestCase.assertTrue(c.isFromLiteral());
		TestCase.assertTrue(c.getGenus().equals(ISymbol.SymbolGenus.VALUE));
	}

	private void assertVariable1(VariableSymbol v)
	{
		TestCase.assertNotNull(v.getFriendlyName());
		TestCase.assertFalse(v.isAtModuleScope());
		TestCase.assertFalse(v.isAggregatePropertyField());
		TestCase.assertTrue(v.isMutable());
		TestCase.assertFalse(v.isIncomingParameter());
		TestCase.assertFalse(v.isPrivate());
		TestCase.assertTrue(v.isPublic());
		TestCase.assertFalse(v.isAggregatePropertyField());
		TestCase.assertFalse(v.isIncomingParameter());
		TestCase.assertFalse(v.isReturningParameter());
		TestCase.assertFalse(v.isInitialised());
	}
}

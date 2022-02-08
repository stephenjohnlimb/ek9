package org.ek9lang.compiler.symbol.support;

import junit.framework.TestCase;
import org.ek9lang.compiler.symbol.ParameterisedTypeSymbol;
import org.ek9lang.compiler.symbol.support.search.TemplateTypeSymbolSearch;
import org.ek9lang.compiler.symbol.support.search.TypeSymbolSearch;
import org.junit.Test;

import java.util.Optional;

/**
 * Going to be very hard to get your head around this as generics and
 * parameterised polymorphism is hard.
 */
public class GenericTypeTest extends AbstractSymbolTestBase
{
	@Test
	public void testTemplateTypeCreation()
	{
		//The type T we are going to use as part of the template type definition
		var t = support.createGenericT("Tee", symbolTable);

		//Now the actual template type that can be parameterised.
		var z = support.createTemplateGenericType("Zee", symbolTable, t);
		symbolTable.define(z);

		//Check we CANNOT find this as a type, but only as a template type.
		TestCase.assertTrue(symbolTable.resolve(new TypeSymbolSearch("Zee")).isEmpty());
		TestCase.assertTrue(symbolTable.resolve(new TypeSymbolSearch("Tee")).isEmpty());

		TestCase.assertTrue(z.isGenericInNature());
		TestCase.assertTrue(symbolTable.resolve(new TemplateTypeSymbolSearch("Zee")).isPresent());
		TestCase.assertEquals("Zee of Tee", z.getFriendlyName());

		//Now lets use that generic Z with a String and then with an Integer

		var stringZeeType = new ParameterisedTypeSymbol(z, symbolTable.resolve(new TypeSymbolSearch("String")), symbolTable);
		symbolTable.define(stringZeeType);
		TestCase.assertEquals("Zee of String", stringZeeType.getFriendlyName());
		//It is now a real concrete type and not generic in nature.
		TestCase.assertFalse(stringZeeType.isGenericInNature());
		//But it is actually a parameterised type
		TestCase.assertTrue(stringZeeType.isAParameterisedType());

		var integerZeeType = new ParameterisedTypeSymbol(z, symbolTable.resolve(new TypeSymbolSearch("Integer")), symbolTable);
		symbolTable.define(integerZeeType);
		TestCase.assertEquals("Zee of Integer", integerZeeType.getFriendlyName());
	}

	/**
	 * Designed to test out if we can work out the concrete types
	 * when using parameterised types within a parameterised type.
	 * i.e. G of (S, T) for example, then inside type G we use S or T in another template type.
	 * i.e. lets say with G we use a List of S or a Function of T. This means when we come to use
	 * actual types like String or Integer for S and T, we must put those in place in G.
	 * But in some cases within G of (S, T) we might use a List of Date i.e. we don;t need to replace!
	 *
	 * HARD, very HARD (well for me anyway).
	 */
	@Test
	public void testCommonT()
	{
		var t = support.createGenericT("Tee", symbolTable);
		var z = support.createTemplateGenericType("Zee", symbolTable, t);
		symbolTable.define(z);

		//Now a new type but also refers to the same 'Tee' i.e. feels like it is defined within Zee
		//So the conceptual 'Tee' we're talking about here is the same 'Tee'
		var p = support.createTemplateGenericType("Pee", symbolTable, t);
		symbolTable.define(p);

		//Now make a parameterised type but not with a concrete type but with Tee again.
		var pDash = new ParameterisedTypeSymbol(p, Optional.of(t), symbolTable);

		var stringType = symbolTable.resolve(new TypeSymbolSearch("String"));
		var integerType = symbolTable.resolve(new TypeSymbolSearch("Integer"));

		TestCase.assertTrue(stringType.isPresent());
		TestCase.assertTrue(integerType.isPresent());

		var stringZeeType = new ParameterisedTypeSymbol(z, stringType, symbolTable);
		symbolTable.define(stringZeeType);

		var integerZeeType = new ParameterisedTypeSymbol(z, integerType, symbolTable);
		symbolTable.define(integerZeeType);

		var types = support.getSuitableParameters(stringZeeType, pDash);

		TestCase.assertFalse(types.isEmpty());
		TestCase.assertTrue(types.get(0).isExactSameType(stringType.get()));

		var concreteTypes = support.getSuitableParameters(stringZeeType, integerZeeType);

		TestCase.assertFalse(concreteTypes.isEmpty());
		TestCase.assertTrue(concreteTypes.get(0).isExactSameType(integerType.get()));
	}
}

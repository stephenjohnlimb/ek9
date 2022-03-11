package org.ek9lang.compiler.symbol.support;

import junit.framework.TestCase;
import org.ek9lang.compiler.files.Module;
import org.ek9lang.compiler.files.Source;
import org.ek9lang.compiler.symbol.*;
import org.ek9lang.compiler.symbol.support.search.MethodSymbolSearch;
import org.ek9lang.compiler.symbol.support.search.SymbolSearch;
import org.ek9lang.compiler.symbol.support.search.TypeSymbolSearch;
import org.ek9lang.compiler.tokenizer.SyntheticToken;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

/**
 * Basic tests for symbols.
 */
public class SymbolsTest extends AbstractSymbolTestBase
{
	@Test
	public void testFullyQualifiedName()
	{
		TestCase.assertFalse(support.isSymbolNameFullyQualified("name"));
		TestCase.assertTrue(support.isSymbolNameFullyQualified("com.name::name"));

		TestCase.assertEquals("com.part", support.getModuleNameIfPresent("com.part::name"));
		TestCase.assertEquals("", support.getModuleNameIfPresent("name"));

		TestCase.assertEquals("name", support.getUnqualifiedName("com.part::name"));
		TestCase.assertEquals("name", support.getUnqualifiedName("name"));
	}

	@Test
	public void testCreateGenericTypeT()
	{
		AggregateSymbol t = support.createGenericT("T", symbolTable);
		TestCase.assertNotNull(t);
		TestCase.assertFalse(t.getSymbolsForThisScope().isEmpty());

		TestCase.assertEquals("T", t.getFriendlyName());

		//Now the idea of a variable inside some sort of generic class/function of type T
		var v1 = new VariableSymbol("v1", t);
		TestCase.assertEquals("v1 as T", v1.getFriendlyName());
	}

	@Test
	public void testDefaultSymbolProperties()
	{
		var s1 = new Symbol("v1", symbolTable.resolve(new TypeSymbolSearch("Integer")));

		TestCase.assertFalse(s1.isPrivate());
		TestCase.assertFalse(s1.isProtected());
		TestCase.assertTrue(s1.isPublic());

		TestCase.assertTrue(s1.isAVariable());
		TestCase.assertFalse(s1.isAMethod());
		TestCase.assertFalse(s1.isAType());
		TestCase.assertFalse(s1.isAControl());
		TestCase.assertFalse(s1.isAFunction());
		TestCase.assertFalse(s1.isAnApplication());
		TestCase.assertFalse(s1.isAPrimitiveType());
		TestCase.assertFalse(s1.isATemplateFunction());
		TestCase.assertFalse(s1.isAConstant());
		TestCase.assertFalse(s1.isATemplateType());

		TestCase.assertFalse(s1.isNullAllowed());
		s1.setNullAllowed(true);
		TestCase.assertTrue(s1.isNullAllowed());

		TestCase.assertFalse(s1.isInjectionExpected());
		s1.setInjectionExpected(true);
		TestCase.assertTrue(s1.isInjectionExpected());

		TestCase.assertFalse(s1.isReferenced());
		s1.setReferenced(true);
		TestCase.assertTrue(s1.isReferenced());

		TestCase.assertFalse(s1.isAParameterisedType());
		TestCase.assertFalse(s1.isGenericInNature());
		TestCase.assertFalse(s1.isGenericTypeParameter());

		TestCase.assertTrue(s1.isMutable());
		s1.setNotMutable();
		TestCase.assertFalse(s1.isMutable());

		TestCase.assertFalse(s1.isLoopVariable());

		TestCase.assertFalse(s1.isIncomingParameter());
		TestCase.assertFalse(s1.isReturningParameter());
		TestCase.assertFalse(s1.isAggregatePropertyField());
	}

	@Test
	public void testSymbolModuleAndSource()
	{
		var variable = new VariableSymbol("v1", symbolTable.resolve(new TypeSymbolSearch("Integer")));
		TestCase.assertFalse(variable.isDevSource());
		TestCase.assertFalse(variable.isLibSource());

		var module = new Module() {
			@Override
			public Source getSource()
			{
				return new Source() {

					@Override
					public String getFileName()
					{
						return "Simulated_file.ek9";
					}

					@Override
					public boolean isLib()
					{
						return true;
					}
					@Override
					public boolean isDev()
					{
						return true;
					}
				};
			}

			@Override
			public String getScopeName()
			{
				return "Simulated Module Scope";
			}
		};

		//Check a null module
		variable.setParsedModule(Optional.ofNullable(null));
		//Now a simulated module
		variable.setParsedModule(Optional.of(module));
		TestCase.assertTrue(variable.isDevSource());
		TestCase.assertTrue(variable.isLibSource());
	}

	@Test
	public void testSymbolCloning()
	{
		var symbol = new Symbol("Sym", symbolTable.resolve(new TypeSymbolSearch("Integer")));
		symbol.setInitialisedBy(new SyntheticToken());
		symbol.setReferenced(true);
		symbol.putSquirrelledData("key1", "value1");
		symbol.putSquirrelledData("key2", "\"value2\"");
		symbol.setNotMutable();
		//Can the symbol possibly be null i.e. never assigned a value.
		symbol.setNullAllowed(true);
		symbol.setInjectionExpected(true);
		//Imagine this is actually part of the ek9 core itself.
		symbol.setEk9Core(true);

		assertBasicSymbolSettings(symbol);

		var cloned = symbol.clone(symbolTable);
		assertBasicSymbolSettings(cloned);
	}

	private void assertBasicSymbolSettings(Symbol s)
	{
		TestCase.assertNotNull(s);
		TestCase.assertNotNull(s.getInitialisedBy());
		TestCase.assertTrue(s.isReferenced());
		TestCase.assertEquals(s.getSquirrelledData("key1"), "value1");
		TestCase.assertEquals(s.getSquirrelledData("key2"), "value2");
		TestCase.assertFalse(s.isMutable());
		TestCase.assertTrue(s.isNullAllowed());
		TestCase.assertTrue(s.isInjectionExpected());
		TestCase.assertTrue(s.isEk9Core());
		TestCase.assertNotNull(s.getSourceToken());
	}

	@Test
	public void testTypeCloningMechanism()
	{
		var from = createBasicAggregate("From");
		//Now add in an additional method.
		Optional<ISymbol> integerType = symbolTable.resolve(new TypeSymbolSearch("Integer"));
		var newMethod1 = support.addPublicMethod(from, "someMethod", List.of(), Optional.of(from));

		TestCase.assertEquals("public From <- someMethod()", newMethod1.getFriendlyName());

		var var1 = new VariableSymbol("v1", integerType);
		var newMethod2 = support.addPublicMethod(from, "someMethod", List.of(var1), Optional.of(from));

		TestCase.assertEquals("public From <- someMethod(v1 as Integer)", newMethod2.getFriendlyName());

		var var2 = new VariableSymbol("v2", integerType);
		var newMethod3 = support.addPublicMethod(from, "someAbstractMethod", List.of(var1, var2), Optional.of(from));
		newMethod3.setMarkedAbstract(true);
		TestCase.assertEquals("public From <- someAbstractMethod(v1 as Integer, v2 as Integer) as abstract", newMethod3.getFriendlyName());

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
		support.addEnumerationMethods(e);
		TestCase.assertFalse(e.getSymbolsForThisScope().isEmpty());
	}

	/**
	 * We're just testing the properties of delegation here.
	 * Not the actual delegation itself. The trait part is omitted here
	 */
	@Test
	public void testSimpleMethodDelegation()
	{
		AggregateSymbol e = new AggregateSymbol("SomeAgg", symbolTable);
		var toStringOnE = support.addPurePublicSimpleOperator(e, "$", symbolTable.resolve(new TypeSymbolSearch("String")));

		AggregateSymbol f = new AggregateSymbol("AdditionalAgg", symbolTable);
		f.define(new VariableSymbol("theDelegate", e));
		var toStringOnF = support.addPurePublicSimpleOperator(f, "$", symbolTable.resolve(new TypeSymbolSearch("String")));
		toStringOnF.setUsedAsProxyForDelegate("theDelegate");

		//So the idea is call method/operator '$' on instance of 'AdditionalAgg' and EK9
		//Would call theDelegate.$() i.e. $ is proxied on 'AdditionalAgg' automatically.
		TestCase.assertFalse(toStringOnE.isUsedAsProxyForDelegate());
		TestCase.assertTrue(toStringOnF.isUsedAsProxyForDelegate());
		TestCase.assertTrue(toStringOnF.isOverride());
		TestCase.assertFalse(toStringOnF.isVirtual());
		TestCase.assertEquals("theDelegate", toStringOnF.getUsedAsProxyForDelegate());
	}

	@Test
	public void testDispatcherProperty()
	{
		//Only testing the fact the property is set - at this point not that it will dispatch!
		AggregateSymbol e = new AggregateSymbol("SomeAgg", symbolTable);
		var param = new VariableSymbol("p1", symbolTable.resolve(new TypeSymbolSearch("Integer")));
		var methodOnE = support.addPublicMethod(e, "someMethod", List.of(param), symbolTable.resolve(new TypeSymbolSearch("String")));
		methodOnE.setMarkedAsDispatcher(true);
		methodOnE.setMarkedNoClone(true);

		TestCase.assertTrue(methodOnE.isMarkedAsDispatcher());
		TestCase.assertTrue(methodOnE.isMarkedNoClone());
	}

	/**
	 * Just really aimed at testing the properties on a method.
	 * So we're not checking resolution of stuff here, just basic props.
	 */
	@Test
	public void testSimpleMethodProperties()
	{
		AggregateSymbol e = new AggregateSymbol("SomeAgg", symbolTable);

		//Add a synthetic comparator operator
		var comparator = support.addComparatorOperator(e, "<>", symbolTable.resolve(new TypeSymbolSearch("Boolean")));

		TestCase.assertFalse(comparator.isSynthetic());
		TestCase.assertFalse(comparator.isVirtual());
		TestCase.assertFalse(comparator.isConstructor());
		TestCase.assertFalse(comparator.isOverride());
		TestCase.assertFalse(comparator.isPrivate());
		TestCase.assertFalse(comparator.isProtected());
		TestCase.assertTrue(comparator.isPublic());
		TestCase.assertTrue(comparator.isOperator());
		TestCase.assertFalse(comparator.isEk9ReturnsThis());
		TestCase.assertTrue(comparator.isReturningSymbolPresent());
		TestCase.assertFalse(comparator.isMarkedNoClone());
		TestCase.assertTrue(comparator.isMarkedPure());
		TestCase.assertTrue(comparator.isSignatureMatchTo(comparator));
		TestCase.assertTrue(comparator.isAConstant());
		TestCase.assertFalse(comparator.isMarkedAbstract());
		TestCase.assertFalse(comparator.isMarkedAsDispatcher());

		TestCase.assertTrue(comparator.isSignatureMatchTo(comparator));
		TestCase.assertTrue(comparator.isParameterSignatureMatchTo(List.of(e)));
		TestCase.assertFalse(comparator.isParameterSignatureMatchTo(List.of()));

		var lt = support.addComparatorOperator(e, "<", symbolTable.resolve(new TypeSymbolSearch("Integer")));
		TestCase.assertFalse(comparator.isSignatureMatchTo(lt));
		TestCase.assertTrue(lt.isParameterSignatureMatchTo(List.of(e)));

		var isSet = support.addPurePublicSimpleOperator(e, "?", symbolTable.resolve(new TypeSymbolSearch("Boolean")));
		TestCase.assertFalse(comparator.isSignatureMatchTo(isSet));

		//Now check we can find the comparator!
		var resolvedComparator = e.resolve(new MethodSymbolSearch(comparator));
		TestCase.assertTrue(resolvedComparator.isPresent());

	}

	@Test
	public void testFunctionProperties()
	{
		Optional<ISymbol> stringType = symbolTable.resolve(new TypeSymbolSearch("String"));
		Optional<ISymbol> integerType = symbolTable.resolve(new TypeSymbolSearch("Integer"));
		var f1 = new FunctionSymbol("f1", symbolTable);
		f1.setReturningSymbol(new VariableSymbol("rtn", integerType));
		//Now add a parameter.
		f1.define(new VariableSymbol("arg1", stringType));

		TestCase.assertNotNull(f1);
		TestCase.assertEquals("public Integer <- f1(arg1 as String)", f1.getFriendlyName());
		TestCase.assertEquals("public Integer <- f1(arg1 as String)", f1.getFriendlyScopeName());
		//Does not have a super.
		TestCase.assertTrue(f1.getSuperFunctionSymbol().isEmpty());

		var f2 = f1.clone(symbolTable);
		TestCase.assertNotNull(f2);
		f2.setName("f2");
		TestCase.assertEquals("public Integer <- f2(arg1 as String)", f2.getFriendlyName());
		TestCase.assertEquals("public Integer <- f2(arg1 as String)", f2.getFriendlyScopeName());
		TestCase.assertTrue(f2.getSuperFunctionSymbol().isEmpty());

		var superF = new FunctionSymbol("superF", symbolTable);
		superF.setReturningSymbol(new VariableSymbol("rtn", integerType));
		//Now add a parameter.
		superF.define(new VariableSymbol("arg1", stringType));
		//So this is just a signature
		superF.setMarkedAbstract(true);
		TestCase.assertNotNull(f2);
		TestCase.assertEquals("public Integer <- superF(arg1 as String) as abstract", superF.getFriendlyName());
		TestCase.assertEquals("public Integer <- superF(arg1 as String) as abstract", superF.getFriendlyScopeName());
		TestCase.assertTrue(superF.getSuperFunctionSymbol().isEmpty());

		//Now lets make a function that extends this super function.
		var f3 = f1.clone(symbolTable);
		TestCase.assertNotNull(f3);
		f3.setName("f3");
		f3.setSuperFunctionSymbol(Optional.of(superF));
		TestCase.assertTrue(f3.getSuperFunctionSymbol().isPresent());
		TestCase.assertEquals("public Integer <- f3(arg1 as String) is superF", f3.getFriendlyName());
	}

	@Test
	public void testFunctionWithCapturedVariables()
	{
		Optional<ISymbol> stringType = symbolTable.resolve(new TypeSymbolSearch("String"));
		Optional<ISymbol> integerType = symbolTable.resolve(new TypeSymbolSearch("Integer"));
		//Going to be a dynamic function
		var f1 = new FunctionSymbol("", symbolTable);
		f1.setReturningSymbol(new VariableSymbol("rtn", integerType));
		//Now add a parameter.
		f1.define(new VariableSymbol("arg1", stringType));

		//OK now lets set up the captured variables
		LocalScope capturedVariables = new LocalScope(symbolTable);
		capturedVariables.define(new VariableSymbol("first", stringType));
		capturedVariables.define(new VariableSymbol("second", integerType));
		f1.setCapturedVariables(capturedVariables);
		f1.setCapturedVariablesVisibility(true);

		TestCase.assertEquals("public Integer <- dynamic function(first as String, second as Integer)(arg1 as String)", f1.getFriendlyScopeName());
		TestCase.assertEquals("public Integer <- dynamic function(first as String, second as Integer)(arg1 as String)", f1.getFriendlyName());

		//Check the cloning works.
		var f2 = f1.clone(symbolTable);
		TestCase.assertEquals("public Integer <- dynamic function(first as String, second as Integer)(arg1 as String)", f2.getFriendlyName());
	}

	@Test
	public void testCallSymbolProperties()
	{
		Optional<ISymbol> stringType = symbolTable.resolve(new TypeSymbolSearch("String"));
		Optional<ISymbol> integerType = symbolTable.resolve(new TypeSymbolSearch("Integer"));
		var call1 = new CallSymbol("call1", symbolTable);

		var f1 = new FunctionSymbol("f1", symbolTable);
		f1.setReturningSymbol(new VariableSymbol("rtn", integerType));
		//Now add a parameter.
		f1.define(new VariableSymbol("arg1", stringType));
		TestCase.assertEquals("call1", call1.getFriendlyName());

		//Now modify the call1; so it actually references the function we want to call.
		call1.setResolvedMethodToCall(f1);
		TestCase.assertEquals("call1 => public Integer <- f1(arg1 as String)", call1.getFriendlyName());

		//clone and check the thing to call is still present.
		var call2 = call1.clone(symbolTable);
		call2.setName("call2");
		TestCase.assertNotNull(call2.getResolvedMethodToCall());
		TestCase.assertEquals("call2 => public Integer <- f1(arg1 as String)", call2.getFriendlyScopeName());
		TestCase.assertEquals("call2 => public Integer <- f1(arg1 as String)", call2.getFriendlyName());
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

		//Now create a variable of that Type.
		var v1 = new VariableSymbol("v1", underTest);
		TestCase.assertEquals("v1 as UnderTest", v1.getFriendlyName());
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
		TestCase.assertNotNull(constructor2);
		return rtn;
	}

	@Test
	public void testExpressionSymbol()
	{
		var expr = new ExpressionSymbol("Expr");

		//Check before we set the type
		TestCase.assertEquals("Unknown <- Expr", expr.getFriendlyName());

		//This is the type this expression can return
		expr.setType(symbolTable.resolve(new TypeSymbolSearch("String")));
		expr.setPromotionRequired(false);
		expr.setUseStringOperator(true);
		assertExpressionSymbolValues(expr);

		var clone = expr.clone(symbolTable);
		assertExpressionSymbolValues(clone);

		TestCase.assertEquals(expr, clone);
		TestCase.assertFalse(expr.equals("AString"));

		var expr2 = new ExpressionSymbol(new VariableSymbol("v1", symbolTable.resolve(new TypeSymbolSearch("String"))));
		TestCase.assertNotNull(expr2);

	}

	private void assertExpressionSymbolValues(ExpressionSymbol expr)
	{
		TestCase.assertFalse(expr.isPromotionRequired());
		TestCase.assertTrue(expr.getType().isPresent());
		TestCase.assertTrue(expr.isUseStringOperator());
		TestCase.assertFalse(expr.isMutable());
		TestCase.assertTrue(expr.isAConstant());
		TestCase.assertEquals("String <- Expr", expr.getFriendlyName());
	}

	@Test
	public void testParamExpressionSymbol()
	{
		var expr = new ParamExpressionSymbol("ParamExpr");

		expr.addParameter(new VariableSymbol("v1", symbolTable.resolve(new TypeSymbolSearch("Integer"))));
		//This is the type this param expression can return
		expr.setType(symbolTable.resolve(new TypeSymbolSearch("String")));
		TestCase.assertTrue(expr.getType().isPresent());

		var clone = expr.clone(symbolTable);
		TestCase.assertTrue(clone.getType().isPresent());
		TestCase.assertEquals(1, clone.getParameters().size());
		TestCase.assertEquals("v1", clone.getParameters().get(0).getName());
		TestCase.assertTrue(clone.getParameters().get(0).getType().isPresent());
		TestCase.assertEquals("Integer", clone.getParameters().get(0).getType().get().getName());

		TestCase.assertEquals("(v1 as Integer)", expr.getFriendlyName());
		TestCase.assertEquals(expr, clone);
		TestCase.assertFalse(expr.equals("AString"));
	}

	@Test
	public void testStreamPipeLineSymbol()
	{
		var expr = new StreamPipeLineSymbol("Pipe");

		//This is the type this pipe can return
		expr.setType(symbolTable.resolve(new TypeSymbolSearch("String")));
		TestCase.assertTrue(expr.getType().isPresent());

		var clone = expr.clone(symbolTable);
		TestCase.assertTrue(clone.getType().isPresent());
		TestCase.assertEquals(expr, clone);
		TestCase.assertFalse(expr.equals("AString"));
	}

	@Test
	public void testVariableSymbol()
	{
		IScope symbolTable = new SymbolTable();
		ISymbol integerType = new AggregateSymbol("Integer", symbolTable);

		//define without type first
		VariableSymbol v1 = new VariableSymbol("v1");
		assertVariable1(v1);
		var clone = v1.clone(symbolTable);
		assertVariable1(clone);
		TestCase.assertEquals(v1, clone);
		TestCase.assertFalse(v1.equals("AString"));

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

		TestCase.assertTrue(integerType.isAssignableTo(v3.getType()));
	}

	@Test
	public void testConstantSymbol()
	{
		IScope symbolTable = new SymbolTable();
		ISymbol integerType = new AggregateSymbol("Integer", symbolTable);
		ConstantSymbol c1 = new ConstantSymbol("1", true);
		c1.setType(integerType);
		c1.setNotMutable();

		assertConstant1(c1);
		var clone = c1.clone(symbolTable);
		assertConstant1(clone);

		TestCase.assertEquals(c1, clone);
		TestCase.assertFalse("AString".equals(c1));

		ConstantSymbol c2 = new ConstantSymbol("1", integerType, true);

		TestCase.assertTrue(c2.isAConstant());
		TestCase.assertTrue(c2.isMutable());
		TestCase.assertTrue(c2.isFromLiteral());
		TestCase.assertEquals(c2.getGenus(), ISymbol.SymbolGenus.VALUE);

		ConstantSymbol c3 = new ConstantSymbol("1", integerType);
		TestCase.assertEquals(c3.getGenus(), ISymbol.SymbolGenus.VALUE);

	}

	private void assertConstant1(ConstantSymbol c)
	{
		TestCase.assertNotNull(c.getFriendlyName());
		TestCase.assertTrue(c.isAConstant());
		TestCase.assertFalse(c.isMutable());
		TestCase.assertTrue(c.isFromLiteral());
		TestCase.assertEquals(c.getGenus(), ISymbol.SymbolGenus.VALUE);
	}

	private void assertVariable1(VariableSymbol v)
	{
		TestCase.assertNotNull(v.getFriendlyName());
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

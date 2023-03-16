package org.ek9lang.compiler.symbol.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.ek9lang.compiler.internals.Module;
import org.ek9lang.compiler.internals.Source;
import org.ek9lang.compiler.symbol.AggregateSymbol;
import org.ek9lang.compiler.symbol.CallSymbol;
import org.ek9lang.compiler.symbol.ConstantSymbol;
import org.ek9lang.compiler.symbol.ExpressionSymbol;
import org.ek9lang.compiler.symbol.FunctionSymbol;
import org.ek9lang.compiler.symbol.IScope;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.LocalScope;
import org.ek9lang.compiler.symbol.MethodSymbol;
import org.ek9lang.compiler.symbol.ParamExpressionSymbol;
import org.ek9lang.compiler.symbol.StreamPipeLineSymbol;
import org.ek9lang.compiler.symbol.Symbol;
import org.ek9lang.compiler.symbol.VariableSymbol;
import org.ek9lang.compiler.symbol.support.search.MethodSymbolSearch;
import org.ek9lang.compiler.symbol.support.search.SymbolSearch;
import org.ek9lang.compiler.symbol.support.search.TypeSymbolSearch;
import org.ek9lang.compiler.tokenizer.SyntheticToken;
import org.junit.jupiter.api.Test;

/**
 * Basic tests for symbols.
 */
final class SymbolsTest extends AbstractSymbolTestBase {

  @Test
  void testNoPromotion() {
    var symbol1 = new Symbol("name1");
    var symbol2 = new Symbol("name2");

    assertFalse(symbol1.isPromotionSupported(symbol2));
  }

  @Test
  void testSimpleSymbolEquality() {
    var symbol1 = new Symbol("name");
    var symbol2 = new Symbol("name");
    var symbol3 = new Symbol("differentName");

    assertEquals(symbol1, symbol2);
    assertEquals(symbol1.hashCode(), symbol2.hashCode());

    assertNotEquals(symbol1, symbol3);
    assertNotEquals(symbol1.hashCode(), symbol3.hashCode());
  }

  @Test
  void testFullyQualifiedName() {
    assertFalse(ISymbol.isQualifiedName("name"));
    assertTrue(ISymbol.isQualifiedName("com.name::name"));

    assertEquals("com.part", ISymbol.getModuleNameIfPresent("com.part::name"));
    assertEquals("", ISymbol.getModuleNameIfPresent("name"));

    assertEquals("name", ISymbol.getUnqualifiedName("com.part::name"));
    assertEquals("name", ISymbol.getUnqualifiedName("name"));
  }

  @Test
  void testCreateGenericTypeT() {
    AggregateSymbol t = support.createGenericT("T", symbolTable);
    assertNotNull(t);
    assertFalse(t.getSymbolsForThisScope().isEmpty());

    assertEquals("T", t.getFriendlyName());

    //Now the idea of a variable inside some sort of generic class/function of type T
    var v1 = new VariableSymbol("v1", t);
    assertEquals("v1 as T", v1.getFriendlyName());
  }

  @Test
  void testDefaultSymbolProperties() {
    var s1 = new Symbol("v1", symbolTable.resolve(new TypeSymbolSearch("Integer")));

    assertJustVariable(s1);

    assertFalse(s1.isNullAllowed());
    s1.setNullAllowed(true);
    assertTrue(s1.isNullAllowed());

    assertFalse(s1.isInjectionExpected());
    s1.setInjectionExpected(true);
    assertTrue(s1.isInjectionExpected());

    assertFalse(s1.isReferenced());
    s1.setReferenced(true);
    assertTrue(s1.isReferenced());

    assertTrue(s1.isMutable());
    s1.setNotMutable();
    assertFalse(s1.isMutable());

    assertFalse(s1.isLoopVariable());

    assertFalse(s1.isIncomingParameter());
    assertFalse(s1.isReturningParameter());
    assertFalse(s1.isAggregatePropertyField());
  }

  @Test
  void testSymbolModuleAndSource() {
    var variable = new VariableSymbol("v1", symbolTable.resolve(new TypeSymbolSearch("Integer")));
    assertFalse(variable.isDevSource());
    assertFalse(variable.isLibSource());

    var module = new Module() {
      @Override
      public Source getSource() {
        return new Source() {

          @Override
          public String getFileName() {
            return "Simulated_file.ek9";
          }

          @Override
          public boolean isLib() {
            return true;
          }

          @Override
          public boolean isDev() {
            return true;
          }
        };
      }

      @Override
      public String getScopeName() {
        return "Simulated Module Scope";
      }
    };

    //Check a null module
    variable.setParsedModule(Optional.empty());
    //Now a simulated module
    variable.setParsedModule(Optional.of(module));
    assertTrue(variable.isDevSource());
    assertTrue(variable.isLibSource());
  }

  @Test
  void testSymbolCloning() {
    var symbol = new Symbol("Sym", symbolTable.resolve(new TypeSymbolSearch("Integer")));
    symbol.setSourceToken(new SyntheticToken());
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

  private void assertBasicSymbolSettings(Symbol s) {
    assertNotNull(s);
    assertNotNull(s.getInitialisedBy());
    assertTrue(s.isReferenced());
    assertEquals("value1", s.getSquirrelledData("key1"));
    assertEquals("value2", s.getSquirrelledData("key2"));
    assertFalse(s.isMutable());
    assertTrue(s.isNullAllowed());
    assertTrue(s.isInjectionExpected());
    assertTrue(s.isEk9Core());
    assertNotNull(s.getSourceToken());
  }

  @Test
  void testTypeCloningMechanism() {
    var from = createBasicAggregate("From");
    //Now add in an additional method.
    Optional<ISymbol> integerType = symbolTable.resolve(new TypeSymbolSearch("Integer"));
    var newMethod1 = support.addPublicMethod(from, "someMethod", List.of(), Optional.of(from));

    assertEquals("public From <- someMethod()", newMethod1.getFriendlyName());

    var var1 = new VariableSymbol("v1", integerType);
    var newMethod2 = support.addPublicMethod(from, "someMethod", List.of(var1), Optional.of(from));

    assertEquals("public From <- someMethod(v1 as Integer)", newMethod2.getFriendlyName());

    var var2 = new VariableSymbol("v2", integerType);
    var newMethod3 =
        support.addPublicMethod(from, "someAbstractMethod", List.of(var1, var2), Optional.of(from));
    newMethod3.setMarkedAbstract(true);
    assertEquals("public From <- someAbstractMethod(v1 as Integer, v2 as Integer) as abstract",
        newMethod3.getFriendlyName());

    //Now lets clone those symbols over from "From" to "To" - but we only want the non-abstract methods.
    //But we should get the methods and also any constructors if From had the constructors.
    //This addNonAbstractMethods only applies to methods that return a type of "From" so not all abstract methods.
    var to = new AggregateSymbol("To", symbolTable);

    support.addNonAbstractMethods(from, to);

    var resolvedMethod1 = to.resolveInThisScopeOnly(new MethodSymbolSearch(newMethod1));
    assertTrue(resolvedMethod1.isPresent());
    var resolvedMethod2 = to.resolveInThisScopeOnly(new MethodSymbolSearch(newMethod2));
    assertTrue(resolvedMethod2.isPresent());

    var resolvedMethod3 = to.resolveInThisScopeOnly(new MethodSymbolSearch(newMethod3));
    assertFalse(resolvedMethod3.isPresent());
  }

  @Test
  void testCreateEnumerationType() {
    AggregateSymbol e = new AggregateSymbol("CheckEnum", symbolTable);
    support.addEnumerationMethods(e);
    assertFalse(e.getSymbolsForThisScope().isEmpty());
  }

  /**
   * We're just testing the properties of delegation here.
   * Not the actual delegation itself. The trait part is omitted here
   */
  @Test
  void testSimpleMethodDelegation() {
    AggregateSymbol e = new AggregateSymbol("SomeAgg", symbolTable);
    var toStringOnE = support.addPurePublicSimpleOperator(e, "$",
        symbolTable.resolve(new TypeSymbolSearch("String")));

    AggregateSymbol f = new AggregateSymbol("AdditionalAgg", symbolTable);
    f.define(new VariableSymbol("theDelegate", e));
    var toStringOnF = support.addPurePublicSimpleOperator(f, "$",
        symbolTable.resolve(new TypeSymbolSearch("String")));
    toStringOnF.setUsedAsProxyForDelegate("theDelegate");

    //So the idea is call method/operator '$' on instance of 'AdditionalAgg' and EK9
    //Would call theDelegate.$() i.e. $ is proxied on 'AdditionalAgg' automatically.
    assertFalse(toStringOnE.isUsedAsProxyForDelegate());
    assertTrue(toStringOnF.isUsedAsProxyForDelegate());
    assertTrue(toStringOnF.isOverride());
    assertEquals("theDelegate", toStringOnF.getUsedAsProxyForDelegate());
  }

  @Test
  void testDispatcherProperty() {
    //Only testing the fact the property is set - at this point not that it will dispatch!
    AggregateSymbol e = new AggregateSymbol("SomeAgg", symbolTable);
    var param = new VariableSymbol("p1", symbolTable.resolve(new TypeSymbolSearch("Integer")));
    var methodOnE = support.addPublicMethod(e, "someMethod", List.of(param),
        symbolTable.resolve(new TypeSymbolSearch("String")));
    methodOnE.setMarkedAsDispatcher(true);
    methodOnE.setMarkedNoClone(true);

    assertTrue(methodOnE.isMarkedAsDispatcher());
    assertTrue(methodOnE.isMarkedNoClone());
  }

  /**
   * Just really aimed at testing the properties on a method.
   * So we're not checking resolution of stuff here, just basic props.
   */
  @Test
  void testSimpleMethodProperties() {
    AggregateSymbol e = new AggregateSymbol("SomeAgg", symbolTable);

    //Add a synthetic comparator operator
    var comparator = support.addComparatorOperator(e, "<>",
        symbolTable.resolve(new TypeSymbolSearch("Boolean")));

    assertFalse(comparator.isSynthetic());
    assertFalse(comparator.isConstructor());
    assertFalse(comparator.isOverride());
    assertFalse(comparator.isPrivate());
    assertFalse(comparator.isProtected());
    assertTrue(comparator.isPublic());
    assertTrue(comparator.isOperator());
    assertFalse(comparator.isEk9ReturnsThis());
    assertTrue(comparator.isReturningSymbolPresent());
    assertFalse(comparator.isMarkedNoClone());
    assertTrue(comparator.isMarkedPure());
    assertTrue(comparator.isSignatureMatchTo(comparator));
    assertTrue(comparator.isConstant());
    assertFalse(comparator.isMarkedAbstract());
    assertFalse(comparator.isMarkedAsDispatcher());

    assertTrue(comparator.isSignatureMatchTo(comparator));
    assertTrue(comparator.isParameterSignatureMatchTo(List.of(e)));
    assertFalse(comparator.isParameterSignatureMatchTo(List.of()));

    var lt =
        support.addComparatorOperator(e, "<", symbolTable.resolve(new TypeSymbolSearch("Integer")));
    assertFalse(comparator.isSignatureMatchTo(lt));
    assertTrue(lt.isParameterSignatureMatchTo(List.of(e)));

    var isSet = support.addPurePublicSimpleOperator(e, "?",
        symbolTable.resolve(new TypeSymbolSearch("Boolean")));
    assertFalse(comparator.isSignatureMatchTo(isSet));

    //Now check we can find the comparator!
    var resolvedComparator = e.resolve(new MethodSymbolSearch(comparator));
    assertTrue(resolvedComparator.isPresent());

  }

  @Test
  void testFunctionProperties() {
    Optional<ISymbol> stringType = symbolTable.resolve(new TypeSymbolSearch("String"));
    Optional<ISymbol> integerType = symbolTable.resolve(new TypeSymbolSearch("Integer"));
    var f1 = new FunctionSymbol("f1", symbolTable);
    f1.setReturningSymbol(new VariableSymbol("rtn", integerType));
    //Now add a parameter.
    f1.define(new VariableSymbol("arg1", stringType));

    assertNotNull(f1);
    assertEquals("Integer <- f1(arg1 as String)", f1.getFriendlyName());
    assertEquals("Integer <- f1(arg1 as String)", f1.getFriendlyScopeName());
    //Does not have a super.
    assertTrue(f1.getSuperFunctionSymbol().isEmpty());

    var f2 = f1.clone(symbolTable);
    assertNotNull(f2);
    f2.setName("f2");
    assertEquals("Integer <- f2(arg1 as String)", f2.getFriendlyName());
    assertEquals("Integer <- f2(arg1 as String)", f2.getFriendlyScopeName());
    assertTrue(f2.getSuperFunctionSymbol().isEmpty());

    var superF = new FunctionSymbol("superF", symbolTable);
    superF.setReturningSymbol(new VariableSymbol("rtn", integerType));
    //Now add a parameter.
    superF.define(new VariableSymbol("arg1", stringType));
    //So this is just a signature
    superF.setMarkedAbstract(true);
    assertNotNull(f2);
    assertEquals("Integer <- superF(arg1 as String) as abstract", superF.getFriendlyName());
    assertEquals("Integer <- superF(arg1 as String) as abstract",
        superF.getFriendlyScopeName());
    assertTrue(superF.getSuperFunctionSymbol().isEmpty());

    //Now lets make a function that extends this super function.
    var f3 = f1.clone(symbolTable);
    assertNotNull(f3);
    f3.setName("f3");
    f3.setSuperFunctionSymbol(Optional.of(superF));
    assertTrue(f3.getSuperFunctionSymbol().isPresent());
    assertEquals("Integer <- f3(arg1 as String) is superF", f3.getFriendlyName());
  }

  @Test
  void testFunctionWithCapturedVariables() {
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

    assertEquals(
        "Integer <- dynamic function(first as String, second as Integer)(arg1 as String)",
        f1.getFriendlyScopeName());
    assertEquals(
        "Integer <- dynamic function(first as String, second as Integer)(arg1 as String)",
        f1.getFriendlyName());

    //Check the cloning works.
    var f2 = f1.clone(symbolTable);
    assertEquals(
        "Integer <- dynamic function(first as String, second as Integer)(arg1 as String)",
        f2.getFriendlyName());
  }

  @Test
  void testCallSymbolProperties() {
    Optional<ISymbol> stringType = symbolTable.resolve(new TypeSymbolSearch("String"));
    Optional<ISymbol> integerType = symbolTable.resolve(new TypeSymbolSearch("Integer"));
    var call1 = new CallSymbol("call1", symbolTable);

    var f1 = new FunctionSymbol("f1", symbolTable);
    f1.setReturningSymbol(new VariableSymbol("rtn", integerType));
    //Now add a parameter.
    f1.define(new VariableSymbol("arg1", stringType));
    assertEquals("call1", call1.getFriendlyName());

    //Now modify the call1; so it actually references the function we want to call.
    call1.setResolvedSymbolToCall(f1);
    assertEquals("call1 => Integer <- f1(arg1 as String)", call1.getFriendlyName());

    //clone and check the thing to call is still present.
    var call2 = call1.clone(symbolTable);
    call2.setName("call2");
    assertNotNull(call2.getResolvedSymbolToCall());
    assertEquals("call2 => Integer <- f1(arg1 as String)", call2.getFriendlyScopeName());
    assertEquals("call2 => Integer <- f1(arg1 as String)", call2.getFriendlyName());
  }

  @Test
  void testAggregateCreation() {
    Optional<ISymbol> stringType = symbolTable.resolve(new TypeSymbolSearch("String"));
    Optional<ISymbol> integerType = symbolTable.resolve(new TypeSymbolSearch("Integer"));

    AggregateSymbol underTest = createBasicAggregate("UnderTest");

    //Should have constructor
    Optional<ISymbol> resolvedMethod =
        underTest.resolveInThisScopeOnly(new MethodSymbolSearch("UnderTest"));
    assertTrue(resolvedMethod.isPresent());

    //Add another constructor that takes a String as an argument
    SymbolSearch search1 = new MethodSymbolSearch("UnderTest")
        .addParameter(new VariableSymbol("arg1", stringType));

    resolvedMethod = underTest.resolveInThisScopeOnly(search1);
    assertTrue(resolvedMethod.isPresent());

    //check constructor with String,Integer params does not exist then create it and check it does.
    SymbolSearch search2 = new MethodSymbolSearch("UnderTest")
        .addParameter(new VariableSymbol("arg1", stringType))
        .addParameter(new VariableSymbol("arg2", integerType));
    resolvedMethod = underTest.resolveInThisScopeOnly(search2);

    assertFalse(resolvedMethod.isPresent());
    support.addSyntheticConstructorIfRequired(underTest);
    //Now that constructor should exist
    resolvedMethod = underTest.resolveInThisScopeOnly(search2);
    assertTrue(resolvedMethod.isPresent());

    //Now create a variable of that Type.
    var v1 = new VariableSymbol("v1", underTest);
    assertEquals("v1 as UnderTest", v1.getFriendlyName());
  }

  private AggregateSymbol createBasicAggregate(String name) {
    AggregateSymbol rtn = new AggregateSymbol(name, symbolTable);
    //Add some fields/properties.
    Optional<ISymbol> stringType = symbolTable.resolve(new TypeSymbolSearch("String"));
    Optional<ISymbol> integerType = symbolTable.resolve(new TypeSymbolSearch("Integer"));
    assertTrue(stringType.isPresent());
    assertTrue(integerType.isPresent());

    //Add a couple of properties.
    rtn.define(new VariableSymbol("v1", stringType));
    rtn.define(new VariableSymbol("v2", integerType));

    support.addConstructor(rtn);
    //Add another constructor that takes a String as an argument
    MethodSymbol constructor2 = support.addConstructor(rtn, stringType);
    assertNotNull(constructor2);
    return rtn;
  }

  @Test
  void testExpressionSymbol() {
    var expr = new ExpressionSymbol("Expr");

    //Check before we set the type
    assertEquals("Unknown <- Expr", expr.getFriendlyName());

    //This is the type this expression can return
    expr.setType(symbolTable.resolve(new TypeSymbolSearch("String")));
    expr.setPromotionRequired(false);
    expr.setUseStringOperator(true);
    assertExpressionSymbolValues(expr);

    var clone = expr.clone(symbolTable);
    assertExpressionSymbolValues(clone);

    assertEquals(expr, clone);
    assertNotEquals("AString", expr.getFriendlyName());

    var expr2 = new ExpressionSymbol(
        new VariableSymbol("v1", symbolTable.resolve(new TypeSymbolSearch("String"))));
    assertNotNull(expr2);

  }

  private void assertExpressionSymbolValues(ExpressionSymbol expr) {
    assertFalse(expr.isPromotionRequired());
    assertTrue(expr.getType().isPresent());
    assertTrue(expr.isUseStringOperator());
    assertFalse(expr.isMutable());
    assertTrue(expr.isConstant());
    assertEquals("String <- Expr", expr.getFriendlyName());
  }

  @Test
  void testParamExpressionSymbol() {
    var expr = new ParamExpressionSymbol("ParamExpr");

    expr.addParameter(
        new VariableSymbol("v1", symbolTable.resolve(new TypeSymbolSearch("Integer"))));
    //This is the type this param expression can return
    expr.setType(symbolTable.resolve(new TypeSymbolSearch("String")));
    assertTrue(expr.getType().isPresent());

    var clone = expr.clone(symbolTable);
    assertTrue(clone.getType().isPresent());
    assertEquals(1, clone.getParameters().size());
    assertEquals("v1", clone.getParameters().get(0).getName());
    assertTrue(clone.getParameters().get(0).getType().isPresent());
    assertEquals("Integer", clone.getParameters().get(0).getType().get().getName());

    assertEquals("(v1 as Integer)", expr.getFriendlyName());
    assertEquals(expr, clone);
    assertNotEquals("AString", expr.getFriendlyName());
  }

  @Test
  void testStreamPipeLineSymbol() {
    var expr = new StreamPipeLineSymbol("Pipe");

    //This is the type this pipe can return
    expr.setType(symbolTable.resolve(new TypeSymbolSearch("String")));
    assertTrue(expr.getType().isPresent());

    var clone = expr.clone(symbolTable);
    assertTrue(clone.getType().isPresent());
    assertEquals(expr, clone);
    assertNotEquals("AString", expr.getFriendlyName());
  }

  @Test
  void testUnableToFindControlSymbol() {
		/*
		While we model for loops, while loops switches etc. as symbols (mainly for scope)
		but also because we may want to use them in expressions, we should not be able to 'search'
		for them in the same way as a type, variable, function, template etc.
		 */
    var search = new SymbolSearch("nonSuch").setSearchType(ISymbol.SymbolCategory.CONTROL);
    var shouldNotBeFound = symbolTable.resolve(search);
    assertFalse(shouldNotBeFound.isPresent());

    //Check cloning also fails to find
    shouldNotBeFound = symbolTable.resolve(new SymbolSearch(search));
    assertFalse(shouldNotBeFound.isPresent());
  }

  @Test
  @SuppressWarnings("java:S5785")
  void testVariableSymbol() {
    IScope symbolTable = new SymbolTable();
    ISymbol integerType = new AggregateSymbol("Integer", symbolTable);

    //define without type first
    VariableSymbol v1 = new VariableSymbol("v1");
    assertVariable1(v1);

    assertEquals(v1, v1);

    VariableSymbol v1RefNull = null;
    assertNotEquals(v1, v1RefNull);

    var clone = v1.clone(symbolTable);
    assertVariable1(clone);
    assertEquals(v1, clone);
    assertNotEquals("AString", v1.getFriendlyName());

    VariableSymbol v2 = new VariableSymbol("v2", Optional.of(integerType));
    v2.setInitialisedBy(new SyntheticToken());
    v2.setIncomingParameter(true);

    assertTrue(v2.isInitialised());

    VariableSymbol v3 = new VariableSymbol("v3", Optional.of(integerType));
    v3.setReturningParameter(true);
    v3.setPrivate(true);
    assertTrue(v3.isReturningParameter());
    assertNotNull(v3.toString());

    VariableSymbol loopVar = new VariableSymbol("loopVar", Optional.of(integerType));
    loopVar.setLoopVariable(true);

    assertTrue(loopVar.isLoopVariable());
    assertTrue(loopVar.clone(symbolTable).isLoopVariable());

    assertNotNull(loopVar.getFriendlyName());
    assertNotNull(loopVar.getFullyQualifiedName());

    assertTrue(integerType.isAssignableTo(v3.getType()));
  }

  @Test
  void testConstantSymbol() {
    IScope symbolTable = new SymbolTable();
    ISymbol integerType = new AggregateSymbol("Integer", symbolTable);
    ConstantSymbol c1 = new ConstantSymbol("1", true);
    c1.setType(integerType);
    c1.setNotMutable();

    assertConstant1(c1);
    var clone = c1.clone(symbolTable);
    assertConstant1(clone);

    assertEquals(c1, clone);
    assertEquals(c1.hashCode(), clone.hashCode());

    assertEquals(c1, clone);
    assertNotEquals("AString", c1.getFriendlyName());

    ConstantSymbol c2 = new ConstantSymbol("1", integerType, true);

    assertTrue(c2.isConstant());
    assertTrue(c2.isMutable());
    assertTrue(c2.isFromLiteral());
    assertEquals(ISymbol.SymbolGenus.VALUE, c2.getGenus());

    ConstantSymbol c3 = new ConstantSymbol("1", integerType);
    assertEquals(ISymbol.SymbolGenus.VALUE, c3.getGenus());
  }

  private void assertConstant1(ConstantSymbol c) {
    assertNotNull(c.getFriendlyName());
    assertTrue(c.isConstant());
    assertFalse(c.isMutable());
    assertTrue(c.isFromLiteral());
    assertEquals(ISymbol.SymbolGenus.VALUE, c.getGenus());
  }

  private void assertVariable1(VariableSymbol v) {
    assertJustVariable(v);
    assertFalse(v.isAggregatePropertyField());
    assertTrue(v.isMutable());
    assertFalse(v.isInitialised());
  }

  private void assertJustVariable(Symbol symbol) {
    assertNotNull(symbol.getFriendlyName());
    assertFalse(symbol.isIncomingParameter());
    assertFalse(symbol.isAggregatePropertyField());
    assertFalse(symbol.isIncomingParameter());
    assertFalse(symbol.isReturningParameter());

    assertFalse(symbol.isPrivate());
    assertFalse(symbol.isProtected());
    assertTrue(symbol.isPublic());

    assertTrue(symbol.isVariable());
    assertFalse(symbol.isMethod());
    assertFalse(symbol.isType());
    assertFalse(symbol.isControl());
    assertFalse(symbol.isFunction());
    assertFalse(symbol.isApplication());
    assertFalse(symbol.isPrimitiveType());
    assertFalse(symbol.isTemplateFunction());
    assertFalse(symbol.isConstant());
    assertFalse(symbol.isTemplateType());
    assertFalse(symbol.isParameterisedType());
    assertFalse(symbol.isGenericInNature());
    assertFalse(symbol.isGenericTypeParameter());

  }
}

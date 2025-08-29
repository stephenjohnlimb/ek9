package org.ek9lang.compiler.symbols.scopes;

import static org.ek9lang.compiler.support.EK9TypeNames.EK9_INTEGER;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_STRING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.search.MethodSymbolSearchResult;
import org.ek9lang.compiler.search.SymbolSearch;
import org.ek9lang.compiler.search.TypeSymbolSearch;
import org.ek9lang.compiler.support.EK9TypeNames;
import org.ek9lang.compiler.support.InternalNameFor;
import org.ek9lang.compiler.support.ParameterizedSymbolCreator;
import org.ek9lang.compiler.support.TypeCreator;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.AggregateWithTraitsSymbol;
import org.ek9lang.compiler.symbols.CallSymbol;
import org.ek9lang.compiler.symbols.CaptureScope;
import org.ek9lang.compiler.symbols.ControlSymbol;
import org.ek9lang.compiler.symbols.ForSymbol;
import org.ek9lang.compiler.symbols.FunctionSymbol;
import org.ek9lang.compiler.symbols.IScope;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.LocalScope;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.symbols.ModuleScope;
import org.ek9lang.compiler.symbols.ScopedSymbol;
import org.ek9lang.compiler.symbols.ServiceOperationSymbol;
import org.ek9lang.compiler.symbols.StackConsistencyScope;
import org.ek9lang.compiler.symbols.StreamCallSymbol;
import org.ek9lang.compiler.symbols.SwitchSymbol;
import org.ek9lang.compiler.symbols.SymbolTable;
import org.ek9lang.compiler.symbols.TrySymbol;
import org.ek9lang.compiler.symbols.VariableSymbol;
import org.ek9lang.compiler.symbols.base.AbstractSymbolTestBase;
import org.ek9lang.core.SharedThreadContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Aimed at testing scopes and in some cases scoped symbols.
 * <p>
 * Does not fully test aggregates and the like, just their scoped natures.
 */
final class ScopesTest extends AbstractSymbolTestBase {

  private final TypeCreator typeCreator = new TypeCreator();

  private final ParameterizedSymbolCreator creator = new ParameterizedSymbolCreator(new InternalNameFor());

  @Test
  void testSymbolTableEquality() {
    var aTable = new SymbolTable("ATable");
    var sameNamedTable = new SymbolTable("ATable");

    assertEquals(aTable, sameNamedTable);
    //Check for self as well.
    assertEquals(aTable, aTable);

    //Create a type in one of those tables and check that they are no longer equal.
    ISymbol stringType = typeCreator.apply("String", aTable);
    assertNotNull(stringType);
    assertNotEquals(aTable, sameNamedTable);
  }

  /**
   * Need to test for symbols declared in a module fully qualified and also non fully qualified searches.
   */
  @Test
  void testModuleNamedSymbolResolution() {
    //Raw none-module named
    var module1SymbolTable = new SymbolTable("");

    //This would be a raw String i.e. not in a moduleScope - so not a true reflection of what we will be doing
    //Hence even fully qualified it will just be String
    ISymbol stringType = typeCreator.apply("String", module1SymbolTable);
    assertEquals(stringType.getName(), stringType.getFullyQualifiedName());

    ISymbol noModuleInteger = typeCreator.apply("Integer", module1SymbolTable);
    assertEquals(noModuleInteger.getName(), noModuleInteger.getFullyQualifiedName());

    //But this Integer is going to be in an actual module so can be fully qualified
    var ek9LangSymbolTable = new SymbolTable(EK9TypeNames.EK9_LANG);
    ISymbol orgEk9LangInteger = typeCreator.apply("Integer", ek9LangSymbolTable);

    assertEquals("Integer", orgEk9LangInteger.getName());
    assertEquals(EK9_INTEGER, orgEk9LangInteger.getFullyQualifiedName());

    //Now check that the noModuleInteger is not considered to be the same.
    assertFalse(noModuleInteger.isExactSameType(orgEk9LangInteger));
    //and the other way.
    assertFalse(orgEk9LangInteger.isExactSameType(noModuleInteger));

    //Search in the no module symbol table
    assertTrue(module1SymbolTable.resolve(new TypeSymbolSearch("Integer")).isPresent());
    assertFalse(module1SymbolTable.resolve(new TypeSymbolSearch(EK9_INTEGER)).isPresent());
  }

  @Test
  void testSameSymbolsInModuleResolve() {
    var ek9LangSymbolTable = new SymbolTable(EK9TypeNames.EK9_LANG);
    ISymbol orgEk9LangInteger = typeCreator.apply("Integer", ek9LangSymbolTable);

    var anotherEk9LangSymbolTable = new SymbolTable(EK9TypeNames.EK9_LANG);
    ISymbol anotherOrgEk9LangInteger = typeCreator.apply("Integer", anotherEk9LangSymbolTable);

    assertTrue(anotherOrgEk9LangInteger.isExactSameType(orgEk9LangInteger));
    //And the other way just to be sure
    assertTrue(orgEk9LangInteger.isExactSameType(anotherOrgEk9LangInteger));
  }

  @Test
  void testFullyQualifiedSearch() {
    var ek9LangSymbolTable = new SymbolTable(EK9TypeNames.EK9_LANG);
    ISymbol orgEk9LangInteger = typeCreator.apply("Integer", ek9LangSymbolTable);
    assertNotNull(orgEk9LangInteger);
    assertFalse(ek9LangSymbolTable.resolve(new TypeSymbolSearch("Integer")).isPresent());
    assertTrue(ek9LangSymbolTable.resolve(new TypeSymbolSearch(EK9_INTEGER)).isPresent());

    //Also check non-resolution of type we have not added.
    assertFalse(ek9LangSymbolTable.resolve(new TypeSymbolSearch(EK9_STRING)).isPresent());
  }

  @Test
  void testLocalScope() {
    var local1 = new LocalScope(symbolTable);
    var local2 = new LocalScope(symbolTable);

    //These will be considered equal
    assertEquals(local1, local2);
    assertEquals(local1, local1);
    assertEquals(local1.hashCode(), local2.hashCode());

    assertNotNull(local1);
    Assertions.assertEquals(IScope.ScopeType.BLOCK, local1.getScopeType());
    assertFalse(local1.isMarkedPure());
    assertTrue(local1.isScopeAMatchForEnclosingScope(symbolTable));
    assertFalse(local1.isScopeAMatchForEnclosingScope(new SymbolTable()));
  }

  @Test
  void testDifferentLocalScopes() {
    //Normally when creating local scopes, we could use the name of the file and line no.
    //as the unique identifier of the local scope.
    var local1 = new LocalScope("l1", symbolTable);
    var local2 = new LocalScope("l2", symbolTable);

    assertNotEquals(local1, local2);
    assertNotEquals(local1.hashCode(), local2.hashCode());
  }

  @Test
  void testEnclosingScopeSymbolResolution() {
    var local1 = new LocalScope(symbolTable);

    var resolved = local1.resolve(new TypeSymbolSearch("Boolean"));
    assertTrue(resolved.isPresent());
    assertEquals("Boolean", resolved.get().getName());
  }

  @Test
  void testBlockLimitingResolution() {
    var local1 = new LocalScope(IScope.ScopeType.NON_BLOCK, "SimulatedAggregate", symbolTable);
    var local2 = new LocalScope(local1);

    //Now if we limit to blocks, should not be able to resolve.
    var notResolved = local2
        .resolve(new TypeSymbolSearch("Boolean").setLimitToBlocks(true));
    assertFalse(notResolved.isPresent());
  }

  @Test
  void testGeneralMethodResolution() {
    var local1 = new LocalScope(IScope.ScopeType.NON_BLOCK, "SimulatedAggregate", symbolTable);

    //Create a method and add.
    var method = new MethodSymbol("aMethod", local1);
    local1.define(method);

    var local2 = new LocalScope(local1);
    //Now lets see if we can find that method in the simulated aggregate
    var results = new MethodSymbolSearchResult();
    results = local2.resolveMatchingMethods(new MethodSymbolSearch("aMethod"), results);

    assertTrue(results.isSingleBestMatchPresent());
    assertTrue(results.getSingleBestMatchSymbol().isPresent());
    assertEquals(method, results.getSingleBestMatchSymbol().get());
  }

  @Test
  void testNamedScope() {
    //It's not really an aggregate scope but let's test that type of setting.
    VariableSymbol v1 =
        new VariableSymbol("v3", symbolTable.resolve(new TypeSymbolSearch("Integer")));

    var local = new LocalScope(IScope.ScopeType.NON_BLOCK, "someName", symbolTable);
    assertNotNull(local);
    local.define(v1);
    assertEquals("someName", local.getScopeName());
    assertEquals(IScope.ScopeType.NON_BLOCK, local.getScopeType());
    assertEquals("someName", local.getFriendlyScopeName());

    var clone = local.clone(symbolTable);
    assertNotNull(clone);
    assertEquals(local, clone);
    assertNotEquals("AString", local.getScopeName());
  }

  @Test
  void testScopedSymbolBasics() {

    var scopedSymbol1 = new ScopedSymbol("scopedSymbol1", symbolTable);
    var alsoNamedScopedSymbol1 = new ScopedSymbol("scopedSymbol1", symbolTable);
    
    assertEquals(scopedSymbol1.hashCode(), alsoNamedScopedSymbol1.hashCode());
    assertEquals(scopedSymbol1, alsoNamedScopedSymbol1);

    //Even though it has the same name, it is a different 'type' of scope.
    var localScope = new LocalScope("scopedSymbol1", symbolTable);
    //noinspection AssertBetweenInconvertibleTypes
    assertNotEquals(scopedSymbol1, localScope);

    assertTrue(scopedSymbol1.getAnySuperTypeOrFunction().isEmpty());

    var results = new MethodSymbolSearchResult();
    results = scopedSymbol1.resolveMatchingMethods(new MethodSymbolSearch("aMethod"), results);
    assertTrue(results.isEmpty());

    results = scopedSymbol1.resolveMatchingMethodsInThisScopeOnly(new MethodSymbolSearch("aMethod"), results);
    assertTrue(results.isEmpty());
  }

  @Test
  void testFindingAggregateScope() {
    var aggregateScope1 =
        new ScopedSymbol(IScope.ScopeType.NON_BLOCK, "aggregateScope", symbolTable);
    assertFalse(aggregateScope1.isMarkedPure());
    assertEquals("aggregateScope as Unknown", aggregateScope1.getFriendlyScopeName());
    assertEquals("aggregateScope", aggregateScope1.getName());
    assertEquals("aggregateScope", aggregateScope1.getScopeName());
    assertNotNull(aggregateScope1.getActualScope());

    var blockScope1 = new ScopedSymbol(IScope.ScopeType.BLOCK, "blockScope", aggregateScope1);

    var local = new LocalScope("JustLocalBlock", blockScope1);

    var foundScope = local.findNearestNonBlockScopeInEnclosingScopes();
    assertTrue(foundScope.isPresent());
    assertEquals(aggregateScope1, foundScope.get());

    assertTrue(blockScope1.isScopeAMatchForEnclosingScope(aggregateScope1));
  }

  @Test
  void testAggregateSymbolScope() {
    //So this would be an actual 'type' like an OOP 'Customer' for example
    assertNotNull(checkScopedSymbol(new AggregateSymbol("aggregate", symbolTable)));
  }

  @Test
  void testAggregateWithTraitsSymbolScope() {
    //This would be an aggregate that implements a number of traits.
    assertNotNull(
        checkScopedSymbol(new AggregateWithTraitsSymbol("aggregateWithTraits", symbolTable)));
  }

  @Test
  void testParameterisedTypeSymbolScope() {
    var zee = new AggregateSymbol("Zee", symbolTable);
    zee.setModuleScope(symbolTable);

    var t = aggregateManipulator.createGenericT("Tee", zee.getFullyQualifiedName(), symbolTable);
    zee.addTypeParameterOrArgument(t);
    symbolTable.define(zee);

    assertEquals("Zee of type Tee", zee.getFriendlyName());

    //Check it is possible resolve 'Tee' from within 'Zee'
    var resolvedTee = zee.resolve(new TypeSymbolSearch("Tee"));
    assertTrue(resolvedTee.isPresent());
    var resolvedString = symbolTable.resolve(new TypeSymbolSearch("String"));
    assertTrue(resolvedString.isPresent());
    //This would be a concrete Zee with a concrete type of String to replace 'Tee'
    var pTypeSymbol = creator.apply(zee, List.of(resolvedString.get()));
    var clonedPTypeSymbol = checkScopedSymbol(pTypeSymbol);
    assertNotNull(clonedPTypeSymbol);
    assertEquals("Zee of type Tee of type String", pTypeSymbol.getFriendlyName());
  }

  @Test
  void testParameterisedFunctionSymbolScope() {
    var fun = new FunctionSymbol("fun", symbolTable);
    fun.setModuleScope(symbolTable);

    var t = aggregateManipulator.createGenericT("Tee", fun.getFullyQualifiedName(), symbolTable);
    fun.addTypeParameterOrArgument(t);
    symbolTable.define(fun);

    //We've not defined the return type of the function, so at this point it will be "Unknown"
    Assertions.assertEquals("Unknown <- fun() of type Tee", fun.getFriendlyName());

    //Check it is possible resolve Tee from within 'fun'
    var resolvedTee = fun.resolve(new TypeSymbolSearch("Tee"));
    Assertions.assertTrue(resolvedTee.isPresent());
    var resolvedString = symbolTable.resolve(new TypeSymbolSearch("String"));
    assertTrue(resolvedString.isPresent());

    fun.setReturningSymbol(
        new VariableSymbol("rtn", symbolTable.resolve(new TypeSymbolSearch("Integer"))));
    Assertions.assertEquals("Integer <- fun() of type Tee", fun.getFriendlyName());

    //This would be a concrete 'fun' with a concrete type of String to replace 'Tee'
    var pFun = creator.apply(fun, List.of(resolvedString.get()));
    assertInstanceOf(FunctionSymbol.class, pFun);
  }

  @Test
  void testStackConsistencyScope() {
    //Used in the phase listeners when there is a duplicate or some case where it is not possible
    //to define a scope. We need the scope stack to be coherent, and so we put a StackConsistencyScope
    //on to the scope stack. It's like a bucket to consume all the stuff that should have been put in a scope.

    VariableSymbol v1 =
        new VariableSymbol("v1", symbolTable.resolve(new TypeSymbolSearch("Integer")));
    VariableSymbol v2 =
        new VariableSymbol("v2", symbolTable.resolve(new TypeSymbolSearch("Integer")));

    var cons1 = new StackConsistencyScope(symbolTable);
    var cons2 = cons1.clone(symbolTable);

    assertEquals(cons1, cons2);
    assertEquals(cons1.hashCode(), cons2.hashCode());
    assertEquals(cons1, cons1);

    var captured = new CaptureScope(symbolTable);

    //Check self equals.
    assertEquals(captured, captured);

    captured.define(v2);
    cons1.setCapturedVariables(captured);
    assertTrue(cons1.getCapturedVariables().isPresent());
    assertTrue(cons2.getCapturedVariables().isEmpty());

    cons1.define(v1);

    var resolvedV1 = cons1.resolveExcludingCapturedVariables(new SymbolSearch("v1"));
    assertTrue(resolvedV1.isPresent());

    var notResolvedV2 = cons1.resolveExcludingCapturedVariables(new SymbolSearch("v2"));
    assertTrue(notResolvedV2.isEmpty());

    var resolvedV2 = cons1.resolve(new SymbolSearch("v2"));
    assertTrue(resolvedV2.isPresent());

    var cloneWithCapture = cons1.clone(symbolTable);
    assertEquals(cons1, cloneWithCapture);

    var resolvedInCloneV2 = cloneWithCapture.resolve(new SymbolSearch("v2"));
    assertTrue(resolvedInCloneV2.isPresent());

    var notResolvedInCapture = cloneWithCapture.resolveExcludingCapturedVariables(new SymbolSearch("v2"));
    assertTrue(notResolvedInCapture.isEmpty());
  }

  /**
   * The capture scope is a bit of a strange beast.
   * In the way it allows resolution from an enclosingScope but conditionally.
   * So when the parser hits the 'enter' event we allow full resolution from within the capture scope,
   * but allow it to resolve from the enclosing scope as well. This is how it can resolve basic types and
   * also variables.
   * But on the 'exit' event, it closes off access to the enclosing scope, so now only the variables defined
   * in the capture scope can be resolved.
   * This means that dynamic functions and classes can employ resolution in the 'side symbol table'
   * - that is the set of captured variables. But dynamic functions and classes are taken out of the normal
   * 'enclosing scope' hierarchy and have an enclosing scope at module level.
   */
  @Test
  void testCaptureScope() {
    //Set up an 'enclosing scope' for the CaptureScope to capture variables from via resolution.
    VariableSymbol i1 =
        new VariableSymbol("i1", symbolTable.resolve(new TypeSymbolSearch("Integer")));
    VariableSymbol s1 =
        new VariableSymbol("s1", symbolTable.resolve(new TypeSymbolSearch("String")));

    var someEnclosingBlockScope = new LocalScope("SomeBlock", symbolTable);
    //Now use these variables within the block.
    someEnclosingBlockScope.define(i1);
    someEnclosingBlockScope.define(s1);

    var captureScope1 = new CaptureScope(someEnclosingBlockScope);
    //So this will be empty, but also closed by default.
    //So in this state it won't even be possible resolve normal types.
    assertTrue(captureScope1.resolve(new TypeSymbolSearch("Integer")).isEmpty());
    //It certainly won't be possible to resolve 'i1'.
    assertTrue(captureScope1.resolve(new SymbolSearch("i1")).isEmpty());

    //Now lets open it up just like the 'enter' event will do and then try again.
    captureScope1.setOpenToEnclosingScope(true);
    assertTrue(captureScope1.resolve(new TypeSymbolSearch("Integer")).isPresent());
    var resolvedI1 = captureScope1.resolve(new SymbolSearch("i1"));
    assertTrue(resolvedI1.isPresent());
    var resolvedS1 = captureScope1.resolve(new SymbolSearch("s1"));
    assertTrue(resolvedS1.isPresent());
    //But note we have not actually 'captured' the variable yet - yes it was possible to resolve it.
    //So this is what the event listener must do just before calling this below on the 'exit' event

    //So if the capture was not a 'named' or used the same name capture, just use the same name.
    var capturedVariableI1 = new VariableSymbol((VariableSymbol) resolvedI1.get());
    captureScope1.define(capturedVariableI1);

    //But if s1 was a named capture as 'essOne', we'd do this
    var capturedVariableEssOne = new VariableSymbol("essOne", resolvedS1.get().getType());
    captureScope1.define(capturedVariableEssOne);

    captureScope1.setOpenToEnclosingScope(false);
    //Now let's simulate the exit event and the developer accidentally thinking that can reference 's1'
    assertTrue(captureScope1.resolve(new SymbolSearch("s1")).isEmpty());

    //But as we have now captured 'i1' with a new variable ot the same type it can still be resolved.
    //But this is really a copy in name of the variable in the outer scope.
    var resolvedCapturedI1 = captureScope1.resolve(new SymbolSearch("i1"));
    assertTrue(resolvedCapturedI1.isPresent());

    //But it can be resolved with its new name.
    var resolvedCapturedEssOne = captureScope1.resolve(new SymbolSearch("essOne"));
    assertTrue(resolvedCapturedEssOne.isPresent());
  }

  /**
   * Only a basic check of the module scope.
   * Rest of tests are via actual parsed ek9 source code (in test scenarios).
   */
  @Test
  void testModuleScope() {

    //Don;t actually parse anything here, just need these to be able to create the ModuleScopes.
    var sharedThreadContext = new SharedThreadContext<>(new CompilableProgram());
    ModuleScope scope1 = new ModuleScope("UnderTest", sharedThreadContext);
    ModuleScope scope2 = new ModuleScope("UnderTest", sharedThreadContext);

    assertEquals(scope1, scope2);
    assertEquals(scope1.hashCode(), scope2.hashCode());
    //And self
    assertEquals(scope1, scope1);
  }

  @Test
  void testControlSymbolScope() {
    assertNotNull(checkScopedSymbol(new ControlSymbol("Control", symbolTable)));
  }

  @Test
  void testScopedSymbolScope() {
    assertNotNull(checkScopedSymbol(
        new ScopedSymbol(IScope.ScopeType.NON_BLOCK, "aggregateScope", symbolTable)));

  }

  @Test
  void testForSymbolScope() {
    assertNotNull(checkScopedSymbol(new ForSymbol(symbolTable)));
  }

  @Test
  void testMethodSymbolScope() {
    assertNotNull(checkScopedSymbol(new MethodSymbol("aMethod", symbolTable)));
  }

  @Test
  void testFunctionSymbolScope() {
    assertNotNull(checkScopedSymbol(new FunctionSymbol("aFunction", symbolTable)));
  }

  @Test
  void testCallSymbolScope() {
    assertNotNull(checkScopedSymbol(new CallSymbol("aMethodCall", symbolTable)));
  }

  @Test
  void testServiceOperationSymbolScope() {
    assertNotNull(
        checkScopedSymbol(new ServiceOperationSymbol("aServiceOperationCall", symbolTable)));
  }

  @Test
  void testStreamCallSymbolScope() {
    assertNotNull(checkScopedSymbol(new StreamCallSymbol("aStreamCall", symbolTable)));
  }

  @Test
  void testSwitchSymbolScope() {
    assertNotNull(checkScopedSymbol(new SwitchSymbol(symbolTable)));
  }

  @Test
  void testTrySymbolScope() {
    assertNotNull(checkScopedSymbol(new TrySymbol(symbolTable)));
  }

  private ScopedSymbol checkScopedSymbol(ScopedSymbol scopedSymbol) {
    assertNotNull(scopedSymbol);
    //basic symbol type check
    assertFalse(scopedSymbol.isFromLiteral());
    assertFalse(scopedSymbol.isMarkedAbstract());

    //So all we are doing here - is checking that we can add some sort of 'thing'
    //Then clone the scoped symbol and ensure the 'thing' we added is also in that.
    scopedSymbol.define(
        new VariableSymbol("check", scopedSymbol.resolve(new TypeSymbolSearch("String"))));
    assertTrue(scopedSymbol.resolve(new SymbolSearch("check")).isPresent());

    var clonedSymbol = scopedSymbol.clone(symbolTable);
    assertTrue(clonedSymbol.resolve(new SymbolSearch("check")).isPresent());

    return clonedSymbol;
  }


}

package org.ek9lang.compiler.symbols;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.ek9lang.compiler.search.AnySymbolSearch;
import org.ek9lang.compiler.search.AnyTypeSymbolSearch;
import org.ek9lang.compiler.search.FunctionSymbolSearch;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.search.MethodSymbolSearchResult;
import org.ek9lang.compiler.search.SymbolSearch;
import org.ek9lang.compiler.search.TemplateFunctionSymbolSearch;
import org.ek9lang.compiler.search.TemplateTypeSymbolSearch;
import org.ek9lang.compiler.search.TypeSymbolSearch;
import org.ek9lang.compiler.support.TypeCreator;
import org.ek9lang.compiler.tokenizer.SyntheticToken;
import org.junit.jupiter.api.Test;

/**
 * Test out the basic operations on the symbol table.
 * The Symbol Table is used in many contexts.
 * So this test is just focussed on the symbol table itself really.
 * But it uses lots of other symbols to check things out.
 * The SymbolTable is a really critical bit of the compiler and needs a range of checks.
 */
final class SymbolTableTest {
  @Test
  void testEquality() {
    var st1 = new SymbolTable();
    var st2 = new SymbolTable();

    assertEquals(st1, st2);
    assertEquals(st1.hashCode(), st2.hashCode());

    var st3 = new SymbolTable("GeneralTable");
    assertNotEquals(st1, st3);
    assertNotEquals(st1.hashCode(), st3.hashCode());

    //actually really to be used in local scopes.
    assertTrue(st1.isTerminatedNormally());
  }

  @Test
  void testSearchEmptySymbolTable() {
    SymbolTable underTest = new SymbolTable();
    assertNotFound(underTest);
  }

  @Test
  void testUnresolvedSymbolTable() {
    //Define some symbols - but not the ones we're going to look up.
    SymbolTable underTest = new SymbolTable();
    underTest.define(new ConstantSymbol("notThisConstant"));
    underTest.define(new MethodSymbol("notThisMethod", underTest));

    underTest.define(new FunctionSymbol("notThisFunction", underTest));
    underTest.define(new AggregateSymbol("notThisType", underTest));

    //This turns the functions/aggregates into Template Generic types
    var functionSymbol = new FunctionSymbol("notThisFunction", underTest);
    functionSymbol.addTypeParameterOrArgument(
        new AggregateSymbol("NoneSuch1", underTest));
    underTest.define(functionSymbol);

    var aggregateSymbol = new AggregateSymbol("notThisType", underTest);
    aggregateSymbol.addTypeParameterOrArgument(
        new AggregateSymbol("NoneSuch2", underTest));
    underTest.define(aggregateSymbol);

    assertNotFound(underTest);
  }

  private void assertNotFound(SymbolTable underTest) {
    assertFalse(underTest.resolve(new AnyTypeSymbolSearch("SomeThing")).isPresent());
    assertFalse(underTest.resolve(new AnyTypeSymbolSearch("Fully.Qualified::SomeThing")).isPresent());

    //By specific search types.
    assertFalse(underTest.resolve(new AnyTypeSymbolSearch("SomeThing")).isPresent());
    assertFalse(underTest.resolve(new AnyTypeSymbolSearch("Fully.Qualified::SomeThing")).isPresent());

    assertFalse(underTest.resolve(new TemplateTypeSymbolSearch("SomeThing")).isPresent());
    assertFalse(
        underTest.resolve(new TemplateTypeSymbolSearch("Fully.Qualified::SomeThing")).isPresent());

    assertFalse(underTest.resolve(new FunctionSymbolSearch("SomeThing")).isPresent());
    assertFalse(
        underTest.resolve(new FunctionSymbolSearch("Fully.Qualified::SomeThing")).isPresent());

    assertFalse(underTest.resolve(new TemplateFunctionSymbolSearch("SomeThing")).isPresent());
    assertFalse(underTest.resolve(new TemplateFunctionSymbolSearch("Fully.Qualified::SomeThing"))
        .isPresent());

    assertFalse(underTest.resolveInThisScopeOnly(new TemplateFunctionSymbolSearch("SomeThing"))
        .isPresent());
    assertFalse(underTest.resolve(new MethodSymbolSearch("SomeThing")).isPresent());
    MethodSymbolSearchResult result = new MethodSymbolSearchResult();
    assertTrue(underTest.resolveMatchingMethods(new MethodSymbolSearch("SomeThing"), result)
        .isEmpty());
  }

  @Test
  void testEncounteredExceptionToken() {
    SymbolTable underTest = new SymbolTable();
    assertNull(underTest.getEncounteredExceptionToken());

    underTest.setEncounteredExceptionToken(new SyntheticToken());
    assertNotNull(underTest.getEncounteredExceptionToken());
  }

  @Test
  void testMethodDefinition() {
    SymbolTable underTest = new SymbolTable();

    //A simple method (I know it's just in a symbol table), don't define any parameters or returns.
    var methodName = "method1";
    underTest.define(new MethodSymbol(methodName, underTest));
    assertMethodPresentInSymbolTable(underTest, methodName);
  }

  @Test
  void testCloneSymbolTable() {
    SymbolTable underTest = new SymbolTable();

    //A simple method (I know it's just in a symbol table), don't define any parameters or returns.
    var methodName = "method1";
    underTest.define(new MethodSymbol(methodName, underTest));
    assertMethodPresentInSymbolTable(underTest, methodName);

    //Now clone but with an empty enclosing scope, we should still be able to find the same method
    assertMethodPresentInSymbolTable(underTest.clone(new SymbolTable()), methodName);
  }

  private void assertMethodPresentInSymbolTable(SymbolTable underTest, String methodName) {
    var anySymbolSearch = new AnySymbolSearch(methodName);
    Optional<ISymbol> searchResult = underTest.resolve(anySymbolSearch);
    assertTrue(searchResult.isPresent());
    assertEquals(methodName, searchResult.get().getName());

    //Now clone search and check again!
    searchResult = underTest.resolve(new AnySymbolSearch(anySymbolSearch));
    assertTrue(searchResult.isPresent());
    assertEquals(methodName, searchResult.get().getName());

    MethodSymbolSearch methodSearch = new MethodSymbolSearch(methodName);
    assertNotNull(methodSearch.toString());
    assertTrue(methodSearch.getAsSymbol().isPresent());

    searchResult = underTest.resolve(methodSearch);
    assertTrue(searchResult.isPresent());
    assertEquals(methodName, searchResult.get().getName());

    // Now clone that search and check again
    searchResult = underTest.resolve(new MethodSymbolSearch(methodSearch));
    assertTrue(searchResult.isPresent());
    assertEquals(methodName, searchResult.get().getName());

    //Should not find as a type.
    searchResult = underTest.resolve(new TypeSymbolSearch(methodName));
    assertFalse(searchResult.isPresent());

    MethodSymbolSearchResult result = new MethodSymbolSearchResult();
    result = underTest.resolveMatchingMethods(new MethodSymbolSearch(methodName), result);
    assertFalse(result.isEmpty());
    assertFalse(result.isAmbiguous());
    assertNotNull(result.toString());
    assertTrue(result.getSingleBestMatchSymbol().isPresent());
  }

  @Test
  void testVariableDefinitionInMethod() {
    SymbolTable globalSymbolTable = new SymbolTable();
    MethodSymbol method1 = new MethodSymbol("method1", globalSymbolTable);
    VariableSymbol variableSymbol = new VariableSymbol("var1", method1);

    method1.define(variableSymbol);

    globalSymbolTable.define(method1);

    //Ok so now have a variable defined in a method in a global symbol table.
    //Find the variable.
    Optional<ISymbol> searchResult =
        method1.resolve(new AnySymbolSearch("var1").setLimitToBlocks(true));
    assertTrue(searchResult.isPresent());
  }

  @Test
  void testMethodWithParameters() {
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
    var paramTypes = symbolSearch.getTypeParameters();
    assertEquals(3, paramTypes.size());
    assertEquals(floatType, paramTypes.get(0));
    assertEquals(integerType, paramTypes.get(1));
    assertEquals(stringType, paramTypes.get(2));

    assertEquals("method1(Float, Integer, String)", symbolSearch.toString());
    Optional<ISymbol> resolvedMethod = globalSymbolTable.resolve(symbolSearch);
    assertTrue(resolvedMethod.isPresent());

    //Now lets try a more details clone of the method symbol search
    resolvedMethod = globalSymbolTable.resolve(new MethodSymbolSearch(symbolSearch));
    assertTrue(resolvedMethod.isPresent());

    symbolSearch = new MethodSymbolSearch("method1");
    resolvedMethod = globalSymbolTable.resolve(symbolSearch);
    //method1 does exist but the search will not find it because no params added.
    assertFalse(resolvedMethod.isPresent());
  }

  @Test
  void testMethodOverLoadedDefinition() {
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
    assertEquals("method1", searchResult.get().getName());

    //Search by using a know method as a prototype for a search.
    searchResult = underTest.resolve(new MethodSymbolSearch(method1));
    assertTrue(searchResult.isPresent());
    assertEquals("method1", searchResult.get().getName());

    //Now search but looking for specific return Type.
    searchResult = underTest.resolve(new MethodSymbolSearch("method1", floatType));
    assertTrue(searchResult.isPresent());
    assertEquals("method1", searchResult.get().getName());

    //Look for same method but returning String, should fail
    searchResult = underTest.resolve(new MethodSymbolSearch("method1", Optional.of(stringType)));
    assertFalse(searchResult.isPresent());

    //Same again with Integer
    searchResult =
        underTest.resolve(new MethodSymbolSearch("method1").setOfTypeOrReturn(integerType));
    assertFalse(searchResult.isPresent());

    //Now define again, should allow as overloading
    underTest.define(new MethodSymbol("method1", underTest));
    searchResult = underTest.resolve(new MethodSymbolSearch("method1"));
    //But now it has become ambiguous and so will fail to resolve.
    assertFalse(searchResult.isPresent());
  }

  @Test
  void testFunctionDefinition() {
    SymbolTable underTest = new SymbolTable();

    //A simple function, don't define any parameters or returns.
    underTest.define(new FunctionSymbol("function1", underTest));
    Optional<ISymbol> searchResult = underTest.resolve(new AnySymbolSearch("function1"));
    assertTrue(searchResult.isPresent());
    assertEquals("function1", searchResult.get().getName());

    var functionSymbolSearch = new FunctionSymbolSearch("function1");
    searchResult = underTest.resolve(functionSymbolSearch);
    assertTrue(searchResult.isPresent());
    assertEquals("function1", searchResult.get().getName());

    //Clone search and try again.
    searchResult = underTest.resolve(new FunctionSymbolSearch(functionSymbolSearch));
    assertTrue(searchResult.isPresent());
    assertEquals("function1", searchResult.get().getName());

    //This turns the functions/aggregates into Template Generic types
    var functionSymbol = new FunctionSymbol("function2", underTest);
    var param = new AggregateSymbol("NoneSuch1", underTest);
    param.setConceptualTypeParameter(true);
    functionSymbol.addTypeParameterOrArgument(param);
    underTest.define(functionSymbol);

    var templateFunctionSymbolSearch = new TemplateFunctionSymbolSearch("function2");
    searchResult = underTest.resolve(templateFunctionSymbolSearch);
    assertTrue(searchResult.isPresent());
    assertEquals("function2", searchResult.get().getName());

    //Clone search and try again
    searchResult = underTest.resolve(new TemplateFunctionSymbolSearch(templateFunctionSymbolSearch));
    assertTrue(searchResult.isPresent());
    assertEquals("function2", searchResult.get().getName());

    //Should not find as a type.
    searchResult = underTest.resolve(new TypeSymbolSearch("function1"));
    assertFalse(searchResult.isPresent());

    searchResult = underTest.resolve(new TypeSymbolSearch("function2"));
    assertFalse(searchResult.isPresent());
  }

  @Test
  void testDuplicateFunctionDefinition() {
    assertThrows(java.lang.RuntimeException.class, () -> {
      SymbolTable underTest = new SymbolTable();

      //duplicate check
      underTest.define(new FunctionSymbol("function1", underTest));
      underTest.define(new FunctionSymbol("function1", underTest));
    });
  }

  @Test
  void testConstantDefinition() {
    SymbolTable underTest = new SymbolTable();
    //We should be able to add it without saying what type it is
    underTest.define(new ConstantSymbol("PI"));
    Optional<ISymbol> searchResult = underTest.resolve(new AnySymbolSearch("PI"));
    assertTrue(searchResult.isPresent());
    assertEquals("PI", searchResult.get().getName());
  }

  @Test
  void testConstantDefinitionInvalidType() {
    SymbolTable underTest = new SymbolTable();
    //We should be able to add it without saying what type it is
    underTest.define(new ConstantSymbol("PI"));
    //But if we search and ask for a specific type of return then it should not be found.
    SymbolSearch search =
        new AnySymbolSearch("PI").setOfTypeOrReturn(new AggregateSymbol("Float", underTest));
    Optional<ISymbol> searchResult = underTest.resolve(search);
    assertFalse(searchResult.isPresent());
  }

  @Test
  void testConstantDefinitionValidType() {
    SymbolTable underTest = new SymbolTable();
    //This time give it a type.
    underTest.define(new ConstantSymbol("PI").setType(new AggregateSymbol("Float", underTest)));
    //But if we search and ask for a specific type of return then it should not be found.
    SymbolSearch search =
        new AnySymbolSearch("PI").setOfTypeOrReturn(new AggregateSymbol("Float", underTest));
    Optional<ISymbol> searchResult = underTest.resolve(search);
    //Now it should be found
    assertTrue(searchResult.isPresent());
    assertEquals("PI", searchResult.get().getName());

    //It should also be found if we don't request a specific return type.
    searchResult = underTest.resolve(new AnySymbolSearch("PI"));
    //Now it should be found
    assertTrue(searchResult.isPresent());
    assertEquals("PI", searchResult.get().getName());
  }

  @Test
  void testDuplicateConstantDefinition() {
    assertThrows(java.lang.RuntimeException.class, () -> {
      SymbolTable underTest = new SymbolTable();
      underTest.define(new ConstantSymbol("PI"));
      //Check duplications detected.
      underTest.define(new ConstantSymbol("PI"));
    });
  }

  @Test
  void testBasicTypeDefinitionAndLookup() {
    final TypeCreator typeCreator = new TypeCreator();

    //Now remember this is just a test of the symbol tables and general resolution
    //So I'm using 'global' as the scope name - when it comes to real ek9 we'd use a specific name.
    SymbolTable underTest = new SymbolTable();
    assertEquals("global", underTest.getScopeName());
    assertEquals("global", underTest.toString());

    assertFalse(underTest.resolve(new TypeSymbolSearch("global::Float")).isPresent());

    ISymbol floatType = typeCreator.apply("Float", underTest);
    assertNotNull(floatType);

    List<ISymbol> symbols = underTest.getSymbolsForThisScope();
    assertEquals(1, symbols.size());
    assertEquals("Float", symbols.get(0).getName());

    symbols = underTest.getSymbolsForThisScopeOfCategory(ISymbol.SymbolCategory.TYPE);
    assertEquals(1, symbols.size());
    assertEquals("Float", symbols.get(0).getName());
    assertEquals("global::Float", symbols.get(0).getFullyQualifiedName());

    //Now do a type symbol search for a type we know must be there
    Optional<ISymbol> searchResult = underTest.resolve(new TypeSymbolSearch("global::Float"));
    assertTrue(searchResult.isPresent());
    assertEquals("Float", searchResult.get().getName());
    assertEquals("global::Float", searchResult.get().getFullyQualifiedName());

    //Now search via fully qualified name
    searchResult = underTest.resolve(new TypeSymbolSearch("global::Float"));
    assertTrue(searchResult.isPresent());
    assertEquals("Float", searchResult.get().getName());

    //Now search via any search.
    searchResult = underTest.resolve(new AnySymbolSearch("global::Float"));
    assertTrue(searchResult.isPresent());
    assertEquals("Float", searchResult.get().getName());

    //And via 'any type' search.
    searchResult = underTest.resolve(new AnyTypeSymbolSearch("global::Float"));
    assertTrue(searchResult.isPresent());
    assertEquals("Float", searchResult.get().getName());

    //Now do a type symbol search for a type we know can't be there
    var typeSymbolSearch = new TypeSymbolSearch("global::Integer");
    searchResult = underTest.resolveInThisScopeOnly(typeSymbolSearch);
    assertFalse(searchResult.isPresent());

    //Clone and try again
    searchResult = underTest.resolveInThisScopeOnly(new TypeSymbolSearch(typeSymbolSearch));
    assertFalse(searchResult.isPresent());

    //Has no enclosing scope so expect false.
    assertFalse(underTest.findNearestNonBlockScopeInEnclosingScopes().isPresent());
    assertFalse(underTest.isScopeAMatchForEnclosingScope(underTest));
    assertFalse(underTest.resolveWithEnclosingScope(new TypeSymbolSearch("global::Float")).isPresent());
  }
}

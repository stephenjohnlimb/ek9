package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.main.phases.CompilationPhase;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.support.GenericsSymbolCheck;
import org.ek9lang.compiler.symbol.support.SymbolCheck;
import org.ek9lang.compiler.symbol.support.SymbolCountCheck;
import org.ek9lang.compiler.symbol.support.SymbolSearchForTest;
import org.ek9lang.compiler.symbol.support.SymbolSearchMapFunction;
import org.ek9lang.compiler.symbol.support.search.FunctionSymbolSearch;
import org.junit.jupiter.api.Test;

/**
 * Just test generics simple declaration use compiles.
 * But also that the resulting parameterized symbols also get put into the correct module.
 */
class GenericsUse1CompilationTest extends FullCompilationTest {

  public GenericsUse1CompilationTest() {
    super("/examples/genericsUse1");
  }

  private final SymbolSearchMapFunction mapFunction = new SymbolSearchMapFunction();
  @Test
  void testReferencePhasedDevelopment() {
    testToPhase(CompilationPhase.REFERENCE_CHECKS);
  }

  /**
   * Checks only the stock ek9 stuff is present before parsing and compiling the source.
   * Checks that specific types that will be created during the compilation list Optional of String
   * do not exist yet as types.
   */
  protected void assertPreConditions(CompilableProgram program) {
    new SymbolCountCheck("org.ek9.lang", 66).test(program);

    var checker = new GenericsSymbolCheck(program, "org.ek9.lang", false);
    checker.accept(new SymbolSearchForTest("Optional", mapFunction.apply(List.of("String"))));
    checker.accept(new SymbolSearchForTest("Supplier", mapFunction.apply(List.of("Integer"))));
  }

  @Override
  protected void assertResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertTrue(compilationResult);
    assertEquals(0, numberOfErrors);

    //So we now expect more types as this ek9 source uses ek9 built in generic types.
    assertEk9(program);
    assertSimpleGenerics(program);
    assertFSMGenerics(program);

    assertExpressionDeclarationWithGenerics(program);
  }

  private void assertEk9(final CompilableProgram program) {
    new SymbolCountCheck("org.ek9.lang", 71).test(program);

    var typeChecker = new GenericsSymbolCheck(program, "org.ek9.lang", true, ISymbol.SymbolCategory.TYPE);
    typeChecker.accept(new SymbolSearchForTest("Optional", mapFunction.apply(List.of("String"))));

    var functionChecker = new GenericsSymbolCheck(program, "org.ek9.lang", true, ISymbol.SymbolCategory.FUNCTION);
    functionChecker.accept(new SymbolSearchForTest("Supplier", mapFunction.apply(List.of("Integer"))));
  }

  private void assertSimpleGenerics(final CompilableProgram program) {
    new SymbolCountCheck(3, "simple.generics.use.one", 8).test(program);

    var justOptionalSymbol =
        program.resolveByFullyQualifiedSearch(new FunctionSymbolSearch("simple.generics.use.one::JustOptional"));
    assertTrue(justOptionalSymbol.isPresent());

    var alsoJustOptionalSymbol =
        program.resolveByFullyQualifiedSearch(new FunctionSymbolSearch("simple.generics.use.one::AlsoJustOptional"));
    assertTrue(alsoJustOptionalSymbol.isPresent());
  }

  private void assertFSMGenerics(final CompilableProgram program) {
    //So we both use and define some generic type in this module.
    new SymbolCountCheck(1, "com.utils.fsm.example", 10).test(program);

    var typeChecker =
        new GenericsSymbolCheck(program, "com.utils.fsm.example", true, ISymbol.SymbolCategory.TYPE);

    typeChecker.accept(new SymbolSearchForTest("FSM", mapFunction.apply(List.of("Integer"))));
    typeChecker.accept(new SymbolSearchForTest("FSM", mapFunction.apply(List.of("CardSuit"))));

    var functionChecker =
        new GenericsSymbolCheck(program, "com.utils.fsm.example", true, ISymbol.SymbolCategory.FUNCTION);
    functionChecker.accept(new SymbolSearchForTest("FSMListener", mapFunction.apply(List.of("Integer"))));
    functionChecker.accept(new SymbolSearchForTest("FSMListener", mapFunction.apply(List.of("CardSuit"))));
  }

  private void assertExpressionDeclarationWithGenerics(final CompilableProgram program) {
    SymbolCheck ek9TypeChecker =
        new SymbolCheck(program, "org.ek9.lang", true, true, ISymbol.SymbolCategory.TYPE);

    var moduleName = "simple.generics.use.four";

    SymbolCheck basicTypeChecker =
        new SymbolCheck(program, moduleName, true, true, ISymbol.SymbolCategory.TEMPLATE_TYPE);
    SymbolCheck basicFunctionChecker =
        new SymbolCheck(program, moduleName, true, true, ISymbol.SymbolCategory.FUNCTION);

    var genericTypeChecker = new GenericsSymbolCheck(program, moduleName, true, ISymbol.SymbolCategory.TYPE);

    //OK now lets see what is present and what is not

    basicTypeChecker.accept("GenericThing");
    basicTypeChecker.accept("GenericMapThing");
    basicFunctionChecker.accept("FunctionWithExplicitGenericConstruction");

    //Just check we can resolve the parameterizing types in the appropriate scope.
    ek9TypeChecker.accept("Date");
    ek9TypeChecker.accept("Millisecond");

    //So we've resolved the building blocks of what we need.
    //Now let's see if the compiler has parsed processed and constructed the new types that
    //should result for this combination.

    //For this we used the full declaration of the type, so it should get created and registered.
    var genericThingOfDateTimeSearch = new SymbolSearchForTest("GenericThing", mapFunction.apply(List.of("DateTime")));
    genericTypeChecker.accept(genericThingOfDateTimeSearch);

    var genericThingOfDurationSearch = new SymbolSearchForTest("GenericThing", mapFunction.apply(List.of("Duration")));
    genericTypeChecker.accept(genericThingOfDurationSearch);

    var genericThingOfMilliSecondSearch = new SymbolSearchForTest("GenericThing", mapFunction.apply(List.of("Millisecond")));
    genericTypeChecker.accept(genericThingOfMilliSecondSearch);

    genericTypeChecker.accept(new SymbolSearchForTest("GenericThing", mapFunction.apply(List.of("Date"))));

    genericTypeChecker.accept(new SymbolSearchForTest("GenericThing", mapFunction.apply(List.of("Float"))));

    var integerSearch = new SymbolSearchForTest("Integer");
    genericTypeChecker.accept(new SymbolSearchForTest("GenericMapThing", List.of(integerSearch, genericThingOfMilliSecondSearch)));

    //We'd expect this no more and no less.
    new SymbolCountCheck(1, moduleName, 9).test(program);
  }
}

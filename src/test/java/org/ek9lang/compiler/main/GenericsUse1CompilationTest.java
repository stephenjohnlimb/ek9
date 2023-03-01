package org.ek9lang.compiler.main;

import static org.ek9lang.compiler.internals.Ek9BuiltinLangSupplier.NUMBER_OF_EK9_SYMBOLS;
import static org.ek9lang.compiler.symbol.support.AggregateFactory.EK9_LANG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.internals.CompilableSource;
import org.ek9lang.compiler.main.phases.CompilationPhase;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.support.GenericsSymbolCheck;
import org.ek9lang.compiler.symbol.support.SymbolCheck;
import org.ek9lang.compiler.symbol.support.SymbolCountCheck;
import org.ek9lang.compiler.symbol.support.SymbolSearchForTest;
import org.ek9lang.compiler.symbol.support.SymbolSearchMapFunction;
import org.ek9lang.compiler.symbol.support.search.FunctionSymbolSearch;
import org.ek9lang.core.threads.SharedThreadContext;
import org.junit.jupiter.api.Test;

/**
 * Just test generics simple declaration use compiles.
 * But also that the resulting parameterized symbols also get put into the correct module.
 * This test calls is doing far too much, need to split out to separate tests.
 */
class GenericsUse1CompilationTest extends FullCompilationTest {

  public GenericsUse1CompilationTest() {
    super("/examples/genericsUse1");
  }

  private final SymbolSearchMapFunction mapFunction = new SymbolSearchMapFunction();

  @Test
  void testPhasedDevelopment() {
    testToPhase(CompilationPhase.EXPLICIT_TYPE_SYMBOL_DEFINITION);
  }

  /**
   * Checks only the stock ek9 stuff is present before parsing and compiling the source.
   * Checks that specific types that will be created during the compilation list Optional of String
   * do not exist yet as types.
   */
  protected void assertPreConditions(CompilableProgram program) {
    new SymbolCountCheck(EK9_LANG, NUMBER_OF_EK9_SYMBOLS).test(program);

    var checker = new GenericsSymbolCheck(program, EK9_LANG, false);
    checker.accept(new SymbolSearchForTest("Optional", mapFunction.apply(List.of("String"))));
    checker.accept(new SymbolSearchForTest("Supplier", mapFunction.apply(List.of("Integer"))));
  }

  @Override
  protected void compilationPhaseCompleted(final CompilationPhase phase, final CompilableSource source,
                                           final SharedThreadContext<CompilableProgram> sharedCompilableProgram) {

    if (phase.equals(CompilationPhase.SYMBOL_DEFINITION)) {
      //Will missing type because BespokeClass declared after GenericThing - so needs a second pass
      var moduleName = "simple.generics.use.four";
      sharedCompilableProgram.accept(program -> {
        //Make some new type checkers to validate presence or absence of types.
        //But we need to check the event we are being called on is for our source and module.
        var parsedModule = program.getParsedModuleForCompilableSource(source);
        if(parsedModule.getModuleName().equals(moduleName)) {
          SymbolCheck templateTypeChecker =
              new SymbolCheck(program, moduleName, true, true, ISymbol.SymbolCategory.TEMPLATE_TYPE);
          SymbolCheck basicTypeChecker =
              new SymbolCheck(program, moduleName, true, true, ISymbol.SymbolCategory.TYPE);
          var missingGenericTypeChecker =
              new GenericsSymbolCheck(program, moduleName, false, ISymbol.SymbolCategory.TYPE);

          //So this template generic class should be present
          templateTypeChecker.accept("GenericThing");
          //Also this basic type class should be present
          basicTypeChecker.accept("BespokeClass");

          //But because BespokeClass was declared after the use ' thingOfBespokeClass <- GenericThing() of BespokeClass'
          //This type will not have been created - yet! - We need another run through the source to accomplish that
          missingGenericTypeChecker.accept(
              new SymbolSearchForTest("GenericThing", new SymbolSearchForTest("BespokeClass")));
        }
      });
    }
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertFalse(compilationResult);
    assertEquals(4, numberOfErrors);

    //So we now expect more types as this ek9 source uses ek9 built in generic types.
    assertSimpleGenerics(program);
    assertFSMGenerics(program);
    assertExpressionDeclarationWithGenerics(program);
    assertEk9(program);
  }

  private void assertEk9(final CompilableProgram program) {
    //Additional polymorphic parameterised types will have been added to the module.

    var typeChecker = new GenericsSymbolCheck(program, EK9_LANG, true, ISymbol.SymbolCategory.TYPE);
    typeChecker.accept(new SymbolSearchForTest("Optional", mapFunction.apply(List.of("String"))));

    var functionChecker = new GenericsSymbolCheck(program, EK9_LANG, true, ISymbol.SymbolCategory.FUNCTION);
    functionChecker.accept(new SymbolSearchForTest("Supplier", mapFunction.apply(List.of("Integer"))));

    var numberOfAdditionalSymbols = 5;
    new SymbolCountCheck(EK9_LANG, NUMBER_OF_EK9_SYMBOLS + numberOfAdditionalSymbols).test(program);
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
        new SymbolCheck(program, EK9_LANG, true, true, ISymbol.SymbolCategory.TYPE);

    var moduleName = "simple.generics.use.four";

    SymbolCheck templateTypeChecker =
        new SymbolCheck(program, moduleName, true, true, ISymbol.SymbolCategory.TEMPLATE_TYPE);

    SymbolCheck basicTypeChecker =
        new SymbolCheck(program, moduleName, true, true, ISymbol.SymbolCategory.TYPE);

    SymbolCheck basicFunctionChecker =
        new SymbolCheck(program, moduleName, true, true, ISymbol.SymbolCategory.FUNCTION);

    var genericTypeChecker = new GenericsSymbolCheck(program, moduleName, true, ISymbol.SymbolCategory.TYPE);

    //OK now lets see what is present and what is not

    templateTypeChecker.accept("GenericThing");
    templateTypeChecker.accept("GenericMapThing");
    basicTypeChecker.accept("BespokeClass");
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

    var genericThingOfMilliSecondSearch =
        new SymbolSearchForTest("GenericThing", mapFunction.apply(List.of("Millisecond")));
    genericTypeChecker.accept(genericThingOfMilliSecondSearch);

    genericTypeChecker.accept(new SymbolSearchForTest("GenericThing", mapFunction.apply(List.of("Date"))));

    genericTypeChecker.accept(new SymbolSearchForTest("GenericThing", mapFunction.apply(List.of("Float"))));

    var integerSearch = new SymbolSearchForTest("Integer");
    genericTypeChecker.accept(
        new SymbolSearchForTest("GenericMapThing", List.of(integerSearch, genericThingOfMilliSecondSearch)));

    //Check for it being present as we have done sufficient passes to now resolve this.
    genericTypeChecker.accept(
        new SymbolSearchForTest("GenericThing", new SymbolSearchForTest("BespokeClass")));

    //We'd expect this no more and no less.
    new SymbolCountCheck(1, moduleName, 11).test(program);
  }
}

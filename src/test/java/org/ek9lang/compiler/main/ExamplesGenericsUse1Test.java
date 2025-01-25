package org.ek9lang.compiler.main;

import static org.ek9lang.compiler.Ek9BuiltinLangSupplier.NUMBER_OF_EK9_SYMBOLS;
import static org.ek9lang.compiler.support.AggregateManipulator.EK9_LANG;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilableSource;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.ek9lang.compiler.support.GenericsSymbolCheck;
import org.ek9lang.compiler.support.SymbolCheck;
import org.ek9lang.compiler.support.SymbolCountCheck;
import org.ek9lang.compiler.support.SymbolSearchConfiguration;
import org.ek9lang.compiler.support.SymbolSearchMapFunction;
import org.ek9lang.compiler.symbols.SymbolCategory;
import org.ek9lang.core.SharedThreadContext;
import org.junit.jupiter.api.Test;

/**
 * Just test generics simple declaration use compiles.
 * But also that the resulting parameterized symbols also get put into the correct module.
 * This test calls is doing far too much, need to split out to separate tests.
 */
class ExamplesGenericsUse1Test extends PhasesTest {

  private final SymbolSearchMapFunction mapFunction = new SymbolSearchMapFunction();

  public ExamplesGenericsUse1Test() {
    super("/examples/genericsUse1");
  }

  @Test
  void testPhasedDevelopment() {
    testToPhase(CompilationPhase.PRE_IR_CHECKS);
  }

  /**
   * Checks only the stock ek9 stuff is present before parsing and compiling the source.
   * Checks that specific types that will be created during the compilation list Optional of String
   * do not exist yet as types.
   */
  protected void assertPreConditions(CompilableProgram program) {
    new SymbolCountCheck(EK9_LANG, NUMBER_OF_EK9_SYMBOLS).test(program);

    //While we do have @ directives, this is a good test just to ensure that precondition of missing types is correct.
    var checker = new GenericsSymbolCheck(program, EK9_LANG, false);
    checker.accept(new SymbolSearchConfiguration("Supplier", mapFunction.apply(List.of("Integer"))));
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
        if (parsedModule.getModuleName().equals(moduleName)) {
          SymbolCheck templateTypeChecker =
              new SymbolCheck(program, moduleName, true, true, SymbolCategory.TEMPLATE_TYPE);
          SymbolCheck basicTypeChecker =
              new SymbolCheck(program, moduleName, true, true, SymbolCategory.TYPE);
          var missingGenericTypeChecker =
              new GenericsSymbolCheck(program, moduleName, false, SymbolCategory.TYPE);

          //So this template generic class should be present
          templateTypeChecker.accept("GenericThing");
          //Also this basic type class should be present
          basicTypeChecker.accept("BespokeClass");

          //But because BespokeClass was declared after the use ' thingOfBespokeClass <- GenericThing() of BespokeClass'
          //This type will not have been created - yet! - We need another run through the source to accomplish that
          missingGenericTypeChecker.accept(
              new SymbolSearchConfiguration("GenericThing", new SymbolSearchConfiguration("BespokeClass")));
        }
      });
    }
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertTrue(compilationResult);
    //So we now expect more types as this ek9 source uses ek9 built in generic types.
    assertSimpleGenerics(program);
    assertFSMGenerics(program);
    assertExpressionDeclarationWithGenerics(program);
    assertEk9(program);
  }

  private void assertEk9(final CompilableProgram program) {
    //Additional polymorphic parameterised types will have been added to the module.
    //Note these are also checked in the EK9 sourcecode via directives.
    var typeChecker = new GenericsSymbolCheck(program, EK9_LANG, true, SymbolCategory.TYPE);
    typeChecker.accept(new SymbolSearchConfiguration("Optional", mapFunction.apply(List.of("String"))));

    var functionChecker = new GenericsSymbolCheck(program, EK9_LANG, true, SymbolCategory.FUNCTION);
    functionChecker.accept(new SymbolSearchConfiguration("Supplier", mapFunction.apply(List.of("Integer"))));

    var numberOfAdditionalSymbols = 11;
    new SymbolCountCheck(EK9_LANG, NUMBER_OF_EK9_SYMBOLS + numberOfAdditionalSymbols).test(program);
  }

  private void assertSimpleGenerics(final CompilableProgram program) {
    //See @ directive check in EK9 source.
    new SymbolCountCheck(2, "simple.generics.use.one", 7).test(program);
  }

  private void assertFSMGenerics(final CompilableProgram program) {
    //So we both use and define some generic type in this module.
    new SymbolCountCheck(1, "com.utils.fsm.example", 12).test(program);
  }

  private void assertExpressionDeclarationWithGenerics(final CompilableProgram program) {
    //We'd expect this no more and no less.
    new SymbolCountCheck(1, "simple.generics.use.four", 11).test(program);
  }
}

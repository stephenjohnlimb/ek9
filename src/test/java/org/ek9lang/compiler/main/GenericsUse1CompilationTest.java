package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.main.phases.CompilationPhase;
import org.ek9lang.compiler.symbol.ISymbol;
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
    checker.accept("Optional", List.of("String"));
    checker.accept("Supplier", List.of("Integer"));
  }

  @Override
  protected void assertResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertTrue(compilationResult);
    assertEquals(0, numberOfErrors);

    //So we now expect more types as this ek9 source uses ek9 built in generic types.
    new SymbolCountCheck("org.ek9.lang", 71).test(program);

    var typeChecker = new GenericsSymbolCheck(program, "org.ek9.lang", true, ISymbol.SymbolCategory.TYPE);
    typeChecker.accept("Optional", List.of("String"));

    var functionChecker = new GenericsSymbolCheck(program, "org.ek9.lang", true, ISymbol.SymbolCategory.FUNCTION);
    functionChecker.accept("Supplier", List.of("Integer"));

    new SymbolCountCheck(3, "simple.generics.use.one", 8).test(program);

    var justOptionalSymbol =
        program.resolveByFullyQualifiedSearch(new FunctionSymbolSearch("simple.generics.use.one::JustOptional"));
    assertTrue(justOptionalSymbol.isPresent());

    var alsoJustOptionalSymbol =
        program.resolveByFullyQualifiedSearch(new FunctionSymbolSearch("simple.generics.use.one::AlsoJustOptional"));
    assertTrue(alsoJustOptionalSymbol.isPresent());

    //So we both use and define some generic type in this module.
    new SymbolCountCheck(1, "com.utils.fsm.example", 10).test(program);

    var exampleTypeChecker =
        new GenericsSymbolCheck(program, "com.utils.fsm.example", true, ISymbol.SymbolCategory.TYPE);

    exampleTypeChecker.accept("FSM", List.of("Integer"));
    exampleTypeChecker.accept("FSM", List.of("CardSuit"));

    var exampleFunctionChecker =
        new GenericsSymbolCheck(program, "com.utils.fsm.example", true, ISymbol.SymbolCategory.FUNCTION);
    exampleFunctionChecker.accept("FSMListener", List.of("Integer"));
    exampleFunctionChecker.accept("FSMListener", List.of("CardSuit"));
  }

}

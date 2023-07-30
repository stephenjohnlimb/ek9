package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.symbols.support.SymbolCountCheck;
import org.junit.jupiter.api.Test;

/**
 * Tests all construct types in basic form, then checks they can be resolved.
 * Also checks the incoming and returning variables can be resolved ok.
 * For properties and methods, there are separate tests, because those involve
 * mor sophisticated checks, like visibility from specific contexts and also the
 * 'pure' nature. So while a method may exist it may not be possible to call it from a specific context.
 * Here, these checks are just simple stuff within a function or a method, can variables be resolved.
 * There's a bit more to this than you'd think because of dynamic classes/functions and also
 * template/genetic types being employed in dynamic classes/functions.
 */
class ExamplesJustResolutionTest extends FullCompilationTest {

  public ExamplesJustResolutionTest() {
    super("/examples/justResolution");
  }

  @Test
  void testPhasedDevelopment() {
    testToPhase(CompilationPhase.FULL_RESOLUTION);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertTrue(compilationResult);
    assertEquals(0, numberOfErrors);

    var mainModuleName = "just.resolution.items";
    var referencingModuleName = "just.reference.items";
    var functionsModuleName = "just.functions.resolution";
    var dynamicFunctionsModuleName = "just.dynamicfunctions.resolution";

    var classesModuleName = "just.classes.resolution";
    new SymbolCountCheck(2, mainModuleName, 4).test(program);
    new SymbolCountCheck(1, referencingModuleName, 1).test(program);
    new SymbolCountCheck(1, functionsModuleName, 6).test(program);
    new SymbolCountCheck(1, dynamicFunctionsModuleName, 16).test(program);
    new SymbolCountCheck(1, classesModuleName, 1).test(program);
  }
}

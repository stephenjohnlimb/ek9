package org.ek9lang.compiler.main;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.support.SymbolCountCheck;

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
class ExamplesJustResolutionTest extends SuccessfulTest {

  public ExamplesJustResolutionTest() {
    super("/examples/justResolution");
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    super.assertFinalResults(compilationResult, numberOfErrors, program);

    final var mainModuleName = "just.resolution.items";
    final var referencingModuleName = "just.reference.items";
    final var functionsModuleName = "just.functions.resolution";
    final var dynamicFunctionsModuleName = "just.dynamicfunctions.resolution";
    final var classesModuleName = "just.classes.resolution";

    new SymbolCountCheck(2, mainModuleName, 4).test(program);
    new SymbolCountCheck(1, referencingModuleName, 1).test(program);
    new SymbolCountCheck(1, functionsModuleName, 6).test(program);
    new SymbolCountCheck(1, dynamicFunctionsModuleName, 16).test(program);
    new SymbolCountCheck(1, classesModuleName, 1).test(program);
  }
}

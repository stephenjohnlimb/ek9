package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.main.phases.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Just test basics all compile.
 */
class BasicsCompilationTest extends FullCompilationTest {

  public BasicsCompilationTest() {
    super("/examples/basics");
  }


  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.EXPLICIT_TYPE_SYMBOL_DEFINITION);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {

    if (!compilationResult) {
      program.getParsedModuleNames().forEach(moduleName -> {
        var mod = program.getParsedModules(moduleName);
        assertNotNull(mod);
        System.err.println("Module [" + mod + "] [" + mod.size() + "]");
        mod.forEach(loadedModule -> {
          var errorListener = loadedModule.getSource().getErrorListener();
          if (errorListener.hasErrors()) {
            System.err.println("Errors in [" + loadedModule + "]");
            var iter = errorListener.getErrors();
            while (iter.hasNext()) {
              var error = iter.next();
              System.err.println(error.getClassification() + ":" + error);
            }
          }
        });
      });
    }

    assertTrue(compilationResult);
    assertEquals(0, numberOfErrors);
  }
}

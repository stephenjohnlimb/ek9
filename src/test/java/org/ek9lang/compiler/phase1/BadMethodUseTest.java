package org.ek9lang.compiler.phase1;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.junit.jupiter.api.Test;

/**
 * Just tests bad method usage on a range of constructs and functions.
 */
class BadMethodUseTest extends PhasesTest {

  public BadMethodUseTest() {
    super("/examples/parseButFailCompile/badMethodAndFunctionUse");
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.SYMBOL_DEFINITION);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertFalse(compilationResult);
    assertFalse(program.getParsedModules("bad.programs.examples").isEmpty());
    assertFalse(program.getParsedModules("bad.traits.examples").isEmpty());
    assertFalse(program.getParsedModules("bad.classes.examples").isEmpty());
    assertFalse(program.getParsedModules("bad.components.examples").isEmpty());
    assertFalse(program.getParsedModules("bad.records.examples").isEmpty());
    assertFalse(program.getParsedModules("bad.functions.examples").isEmpty());
    assertFalse(program.getParsedModules("bad.dynamicclasses.examples").isEmpty());
  }
}
package org.ek9lang.compiler.phase2;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.FullCompilationTest;
import org.junit.jupiter.api.Test;

/**
 * Just tests bad operator definition/usage on a range of constructs.
 */
class BadOperatorUseFullCompilationTest extends FullCompilationTest {

  public BadOperatorUseFullCompilationTest() {
    super("/examples/parseButFailCompile/badOperatorUse");
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.EXPLICIT_TYPE_SYMBOL_DEFINITION);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertFalse(compilationResult);
    //Initially I did not think I'd created that many operators. Now I ss I have, I think they are all
    //needed and have value in terms of forcing naming and semantics for specific operations.
    assertFalse(program.getParsedModules("good.classes.operators.examples").isEmpty());
    assertFalse(program.getParsedModules("bad.classes.operators.examples1").isEmpty());
    assertFalse(program.getParsedModules("bad.classes.operators.examples2").isEmpty());
    assertFalse(program.getParsedModules("bad.classes.operators.examples3").isEmpty());
    assertFalse(program.getParsedModules("bad.classes.operators.examples4").isEmpty());
    assertFalse(program.getParsedModules("bad.classes.operators.examples5").isEmpty());
    assertFalse(program.getParsedModules("bad.dynamicclasses.operators.examples").isEmpty());

    assertFalse(program.getParsedModules("good.traits.operators.examples").isEmpty());
    assertFalse(program.getParsedModules("bad.traits.operators.examples").isEmpty());

    assertFalse(program.getParsedModules("good.components.operators.examples").isEmpty());
    assertFalse(program.getParsedModules("bad.components.operators.examples").isEmpty());

    assertFalse(program.getParsedModules("good.records.operators.examples").isEmpty());
    assertFalse(program.getParsedModules("bad.records.operators.examples").isEmpty());

    assertFalse(program.getParsedModules("bad.abstractuse.example").isEmpty());

  }
}

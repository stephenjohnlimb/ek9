package org.ek9lang.compiler.phase2;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.junit.jupiter.api.Test;

/**
 * Just tests bad operator definition/usage on a range of constructs.
 */
class BadOperatorUseTest extends PhasesTest {

  public BadOperatorUseTest() {
    super("/examples/parseButFailCompile/badOperatorUse",
        List.of("good.classes.operators.examples",
            "bad.classes.operators.examples1",
            "bad.classes.operators.examples2",
            "bad.classes.operators.examples3",
            "bad.classes.operators.examples4",
            "bad.classes.operators.examples5",
            "bad.dynamicclasses.operators.examples",
            "good.traits.operators.examples",
            "bad.traits.operators.examples",
            "good.components.operators.examples",
            "bad.components.operators.examples",
            "good.records.operators.examples",
            "bad.records.operators.examples",
            "bad.abstractuse.example",
            "missing.operators.examples",
            "bad.defaultoperators.record.examples"));
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

  }
}
